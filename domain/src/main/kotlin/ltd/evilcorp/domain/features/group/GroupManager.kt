package ltd.evilcorp.domain.features.group
import ltd.evilcorp.domain.core.network.Log
import ltd.evilcorp.domain.features.chat.ChatManager

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.IToxGroupManager
import ltd.evilcorp.domain.core.network.IToxProfile
import ltd.evilcorp.domain.core.network.bytesToHex
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository

private const val GROUP_MIGRATION_FLOW_CAPACITY = 10
private const val CHAT_ID_RETRY_ATTEMPTS = 100
private const val CHAT_ID_RETRY_DELAY_MS = 10L

@Singleton
class GroupManager @Inject constructor(
    internal val scope: CoroutineScope,
    internal val groupRepository: IGroupRepository,
    internal val contactRepository: IContactRepository,
    internal val chatManager: ChatManager,
    internal val messageRepository: IMessageRepository,
    internal val tox: IToxGroupManager,
    internal val toxProfile: IToxProfile,
    private val connectionScheduler: IGroupConnectionScheduler,
) {
    var activeGroup = ""
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                scope.launch {
                    groupRepository.setHasUnreadMessages(value, false)
                }
            }
        }

    private val _pendingInvite = MutableStateFlow<GroupInvite?>(null)
    val pendingInvite: Flow<GroupInvite?> = _pendingInvite

    private val _connectionStatuses = MutableStateFlow<Map<String, GroupConnectionStatus>>(emptyMap())
    val connectionStatuses: Flow<Map<String, GroupConnectionStatus>> = _connectionStatuses

    private val _groupMigratedEvent = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = GROUP_MIGRATION_FLOW_CAPACITY)
    val groupMigratedEvent: SharedFlow<Pair<String, String>> = _groupMigratedEvent.asSharedFlow()

    fun connectionStatus(chatId: String): GroupConnectionStatus =
        _connectionStatuses.value[chatId] ?: GroupConnectionStatus.Disconnected

    fun setConnectionStatus(chatId: String, status: GroupConnectionStatus) {
        _connectionStatuses.value = _connectionStatuses.value + (chatId to status)
    }

    fun removeConnectionStatus(chatId: String) {
        _connectionStatuses.value = _connectionStatuses.value - chatId
    }

    fun setPendingInvite(invite: GroupInvite?) {
        _pendingInvite.value = invite
    }

    fun getPendingInvite(): GroupInvite? = _pendingInvite.value

    fun cancelReconnect(chatId: String) {
        connectionScheduler.cancelReconnect(chatId)
    }

    fun notifyGroupMigrated(oldChatId: String, newChatId: String) {
        scope.launch {
            _groupMigratedEvent.emit(Pair(oldChatId, newChatId))
        }
    }

    fun checkAndUpdateGroupMetadata(chatId: String) {
        scope.launch {
            val group = groupRepository.get(chatId).firstOrNull() ?: return@launch
            if (group.groupNumber < 0) return@launch
            
            if (group.name.isEmpty() || group.name == "Unknown Group" || group.name.startsWith("unknown_")) {
                val groupNameBytes = tox.groupGetName(group.groupNumber)
                val groupName = groupNameBytes?.decodeToString()
                if (!groupName.isNullOrBlank() && groupName != "Unknown Group") {
                    groupRepository.setName(chatId, groupName)
                }
            }

            val peers = groupRepository.getPeers(chatId).firstOrNull() ?: emptyList()
            peers.forEach { peer ->
                if (peer.publicKey.isEmpty() && peer.peerId >= 0 && !peer.isOurselves) {
                    val peerKeyBytes = tox.groupPeerGetPublicKey(group.groupNumber, peer.peerId)
                    val peerKey = peerKeyBytes?.bytesToHex()?.uppercase() ?: ""
                    if (peerKey.isNotEmpty()) {
                        val updatedPeer = peer.copy(publicKey = peerKey)
                        groupRepository.addPeer(updatedPeer)
                        Log.i("GroupManager", "Synchronously updated empty publicKey for peer ${peer.name} (${peer.peerId}) -> $peerKey")
                    }
                }
            }
        }
    }

    fun reconnectAll() {
        connectionScheduler.reconnectAll()
    }

    fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {
        connectionScheduler.scheduleAutoReconnect(chatId, groupNumber)
    }

    fun resendPendingMessages(chatId: String) {
        scope.launch {
            val g = groupRepository.get(chatId).firstOrNull() ?: return@launch
            val unsent = groupRepository.getUnsentMessages(chatId)
            if (unsent.isEmpty()) return@launch
            Log.i("GroupManager", "Resending ${unsent.size} pending messages to $chatId")
            for (msg in unsent) {
                val newId = tox.groupSendMessage(
                    g.groupNumber,
                    mapType(msg.type),
                    msg.message.toByteArray(),
                )
                if (newId >= 0) {
                    groupRepository.setCorrelationId(msg.id, newId)
                } else {
                    Log.w("GroupManager", "Failed to resend message ${msg.id} to $chatId")
                }
            }
        }
    }

    suspend fun acceptInvite() = withContext(Dispatchers.IO) {
        val invite = _pendingInvite.value ?: return@withContext
        _pendingInvite.value = null
        val selfName = toxProfile.getName()
        joinGroup(invite.friendNo, invite.inviteData, selfName)
    }

    fun declineInvite() {
        _pendingInvite.value = null
    }

    fun getDefaultSelfName(): String {
        return toxProfile.getName().ifEmpty { "User" }
    }

    fun getAll(): Flow<List<Group>> = groupRepository.getAll()

    fun get(chatId: String): Flow<Group?> = groupRepository.get(chatId)

    suspend fun createGroup(
        privacyState: GroupPrivacyState,
        groupName: String,
        selfName: String,
        password: String? = null,
    ): Int {
        val toxPrivacyState = when (privacyState) {
            GroupPrivacyState.Public -> ToxGroupPrivacyState.PUBLIC
            GroupPrivacyState.Private -> ToxGroupPrivacyState.PRIVATE
        }
        val groupNumber = tox.groupNew(
            toxPrivacyState,
            groupName.toByteArray(),
            selfName.toByteArray(),
        )

        if (groupNumber >= 0) {
            var chatIdBytes = tox.groupGetChatId(groupNumber)
            var attempts = 0
            while (chatIdBytes == null && attempts < CHAT_ID_RETRY_ATTEMPTS) {
                delay(CHAT_ID_RETRY_DELAY_MS)
                chatIdBytes = tox.groupGetChatId(groupNumber)
                attempts++
            }
            val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: "unknown_$groupNumber"

            val selfPeerId = tox.groupSelfGetPeerId(groupNumber)
            val selfRole = tox.groupSelfGetRole(groupNumber)

            if (privacyState == GroupPrivacyState.Private && !password.isNullOrEmpty()) {
                tox.groupSetPassword(groupNumber, password.toByteArray())
            }

            val group = Group(
                chatId = chatId,
                name = groupName,
                privacyState = privacyState,
                passwordProtected = !password.isNullOrEmpty(),
                peerCount = 1,
                selfPeerId = selfPeerId,
                selfRole = selfRole.name,
                groupNumber = groupNumber,
                connected = true,
            )
            groupRepository.add(group)
            setConnectionStatus(chatId, GroupConnectionStatus.Connected)

            val ourPeer = GroupPeer(
                groupChatId = chatId,
                peerId = selfPeerId,
                name = selfName,
                publicKey = toxProfile.publicKey.string(),
                role = selfRole.name,
                isOurselves = true,
            )
            groupRepository.addPeer(ourPeer)
        }

        return groupNumber
    }

    suspend fun joinGroup(
        friendNo: Int,
        inviteData: ByteArray,
        selfName: String,
        password: String? = null,
    ): Int = withContext(Dispatchers.IO) {
        val groupNumber = tox.groupJoin(
            friendNo,
            inviteData,
            selfName.toByteArray(),
            password?.toByteArray(),
        )

        if (groupNumber >= 0) {
            var chatIdBytes = tox.groupGetChatId(groupNumber)
            var attempts = 0
            while (chatIdBytes == null && attempts < CHAT_ID_RETRY_ATTEMPTS) {
                delay(CHAT_ID_RETRY_DELAY_MS)
                chatIdBytes = tox.groupGetChatId(groupNumber)
                attempts++
            }
            val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: "unknown_$groupNumber"
            Log.i("GroupManager", "Joined group number $groupNumber, chatId = $chatId (attempts = $attempts)")

            val groupNameBytes = tox.groupGetName(groupNumber)
            val groupName = groupNameBytes?.decodeToString() ?: "Unknown Group"

            val selfPeerId = tox.groupSelfGetPeerId(groupNumber)
            val selfRole = tox.groupSelfGetRole(groupNumber)

            val group = Group(
                chatId = chatId,
                name = groupName,
                selfPeerId = selfPeerId,
                selfRole = selfRole.name,
                groupNumber = groupNumber,
                connected = false,
            )
            groupRepository.add(group)
            setConnectionStatus(chatId, GroupConnectionStatus.Connecting)

            val ourPeer = GroupPeer(
                groupChatId = chatId,
                peerId = selfPeerId,
                name = selfName,
                publicKey = toxProfile.publicKey.string(),
                role = selfRole.name,
                isOurselves = true,
            )
            groupRepository.addPeer(ourPeer)
        }

        groupNumber
    }

    internal fun mapType(type: MessageType): ToxMessageType = when (type) {
        MessageType.Normal -> ToxMessageType.NORMAL
        MessageType.Action -> ToxMessageType.ACTION
        else -> ToxMessageType.NORMAL
    }
}
