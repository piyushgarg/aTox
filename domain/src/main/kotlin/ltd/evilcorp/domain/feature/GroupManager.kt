package ltd.evilcorp.domain.feature

import java.util.Date
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.model.Group
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.GroupMessage
import ltd.evilcorp.domain.model.GroupPeer
import ltd.evilcorp.domain.model.GroupPrivacyState
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.repository.IContactRepository
import ltd.evilcorp.domain.repository.IGroupRepository
import ltd.evilcorp.domain.repository.IMessageRepository
import ltd.evilcorp.domain.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.tox.enums.ToxMessageType
import ltd.evilcorp.domain.tox.ITox
import ltd.evilcorp.domain.tox.hexToBytes
import ltd.evilcorp.domain.tox.bytesToHex

enum class GroupConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
}

data class GroupInvite(
    val friendNo: Int,
    val inviteData: ByteArray,
    val groupName: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupInvite) return false
        return friendNo == other.friendNo && inviteData.contentEquals(other.inviteData) && groupName == other.groupName
    }

    override fun hashCode(): Int {
        var result = friendNo
        result = 31 * result + inviteData.contentHashCode()
        result = 31 * result + groupName.hashCode()
        return result
    }
}

@Singleton
class GroupManager @Inject constructor(
    private val scope: CoroutineScope,
    private val groupRepository: IGroupRepository,
    private val contactRepository: IContactRepository,
    private val chatManager: ChatManager,
    private val messageRepository: IMessageRepository,
    private val tox: ITox,
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

    private val _groupMigratedEvent = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 10)
    val groupMigratedEvent: SharedFlow<Pair<String, String>> = _groupMigratedEvent.asSharedFlow()

    fun connectionStatus(chatId: String): GroupConnectionStatus =
        _connectionStatuses.value[chatId] ?: GroupConnectionStatus.Disconnected

    fun setConnectionStatus(chatId: String, status: GroupConnectionStatus) {
        _connectionStatuses.value = _connectionStatuses.value + (chatId to status)
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
            
            // 1. Check and update the group name if it hasn't been received over the network yet
            if (group.name.isEmpty() || group.name == "Unknown Group" || group.name.startsWith("unknown_")) {
                val groupNameBytes = tox.groupGetName(group.groupNumber)
                val groupName = groupNameBytes?.decodeToString()
                if (!groupName.isNullOrBlank() && groupName != "Unknown Group") {
                    groupRepository.setName(chatId, groupName)
                }
            }

            // 2. Check and update empty publicKeys of group peers to load avatars
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
        val selfName = tox.getName()
        joinGroup(invite.friendNo, invite.inviteData, selfName)
    }

    fun declineInvite() {
        _pendingInvite.value = null
    }

    fun getDefaultSelfName(): String {
        return tox.getName().ifEmpty { "User" }
    }

    fun getAll(): Flow<List<Group>> = groupRepository.getAll()

    fun get(chatId: String): Flow<Group?> = groupRepository.get(chatId)

    fun createGroup(
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
            val chatIdBytes = tox.groupGetChatId(groupNumber)
            val chatId = chatIdBytes?.toHexString() ?: "unknown_$groupNumber"

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
                publicKey = tox.publicKey.string(),
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
            while (chatIdBytes == null && attempts < 100) {
                delay(10)
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
                publicKey = tox.publicKey.string(),
                role = selfRole.name,
                isOurselves = true,
            )
            groupRepository.addPeer(ourPeer)
        }

        groupNumber
    }

    suspend fun joinGroupWithBytes(
        friendPublicKey: String,
        inviteDataHex: String,
        selfName: String,
        password: String? = null,
    ): Int {
        val pk = PublicKey(friendPublicKey)
        val friendNo = tox.getFriendNumber(pk)
        if (friendNo < 0) return -5 // Friend not found

        val inviteData = try {
            inviteDataHex.hexToBytes()
        } catch (e: Exception) {
            return -4 // Invalid hex format
        }

        return joinGroup(friendNo, inviteData, selfName, password)
    }

    suspend fun leaveGroup(chatId: String) = withContext(Dispatchers.IO) {
        cancelReconnect(chatId)
        val g = groupRepository.get(chatId).firstOrNull()
        g?.let {
            if (it.groupNumber >= 0) {
                tox.groupLeave(it.groupNumber)
            }
            groupRepository.deleteAllPeers(it.chatId)
            groupRepository.deleteByChatId(it.chatId)
            _connectionStatuses.value = _connectionStatuses.value - it.chatId
        }
    }

    suspend fun sendMessage(chatId: String, message: String, type: MessageType = MessageType.Normal) = withContext(Dispatchers.IO) {
        val g = groupRepository.get(chatId).firstOrNull() ?: return@withContext
        val status = connectionStatus(chatId)
        val toxType = mapType(type)

        if (status == GroupConnectionStatus.Connected) {
            val msgId = tox.groupSendMessage(
                g.groupNumber,
                toxType,
                message.toByteArray(),
            )
            val groupMsg = GroupMessage(
                groupChatId = chatId,
                peerId = g.selfPeerId,
                senderName = tox.getName(),
                message = message,
                sender = Sender.Sent,
                type = type,
                correlationId = if (msgId >= 0) msgId else -1,
                timestamp = Date().time,
            )
            groupRepository.addMessage(groupMsg)
            if (msgId < 0) {
                Log.w("GroupManager", "sendMessage failed for $chatId, queued for resend")
            }
        } else {
            val groupMsg = GroupMessage(
                groupChatId = chatId,
                peerId = g.selfPeerId,
                senderName = tox.getName(),
                message = message,
                sender = Sender.Sent,
                type = type,
                correlationId = -1,
                timestamp = Date().time,
            )
            groupRepository.addMessage(groupMsg)
        }
    }

    private fun mapType(type: MessageType): ToxMessageType = when (type) {
        MessageType.Normal -> ToxMessageType.NORMAL
        MessageType.Action -> ToxMessageType.ACTION
        else -> ToxMessageType.NORMAL
    }

    suspend fun setTopic(chatId: String, topic: String) = withContext(Dispatchers.IO) {
        val g = groupRepository.get(chatId).firstOrNull()
        g?.let {
            if (it.groupNumber >= 0) {
                tox.groupSetTopic(it.groupNumber, topic.toByteArray())
                groupRepository.setTopic(chatId, topic)
            }
        }
    }

    fun messagesFor(chatId: String): Flow<List<GroupMessage>> = groupRepository.getMessages(chatId)

    fun getPeers(chatId: String): Flow<List<GroupPeer>> = groupRepository.getPeers(chatId)

    suspend fun clearHistory(chatId: String) = withContext(Dispatchers.IO) {
        groupRepository.deleteMessages(chatId)
        groupRepository.setLastMessage(chatId, 0)
    }

    suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        groupRepository.deleteMessage(id)
    }

    suspend fun setDraft(chatId: String, draft: String) = withContext(Dispatchers.IO) {
        groupRepository.setDraftMessage(chatId, draft)
    }

    suspend fun getChatId(chatId: String): String? = withContext(Dispatchers.IO) {
        groupRepository.get(chatId).firstOrNull()?.chatId
    }

    suspend fun getChatIdByGroupNumber(groupNumber: Int): String? = withContext(Dispatchers.IO) {
        groupRepository.findChatIdByGroupNumber(groupNumber)
    }

    suspend fun inviteFriend(chatId: String, friendPublicKey: String): Boolean = withContext(Dispatchers.IO) {
        val group = groupRepository.get(chatId).firstOrNull()
        if (group != null && group.groupNumber >= 0) {
            val pk = PublicKey(friendPublicKey)
            val friendNumber = tox.getFriendNumber(pk)
            
            // Always display a neat interactive invite card in our own chat (sender's side)
            val inviteText = "[GROUP_INVITE:${group.name}|${group.chatId}]"
            messageRepository.add(
                Message(
                    publicKey = friendPublicKey.lowercase(),
                    message = inviteText,
                    sender = Sender.Sent,
                    type = MessageType.Normal,
                    correlationId = 0,
                    timestamp = java.util.Date().time
                )
            )
            
            if (friendNumber >= 0) {
                val contact = contactRepository.get(friendPublicKey).firstOrNull()
                val isOnline = contact?.connectionStatus != ConnectionStatus.None
                if (isOnline) {
                    tox.groupInviteSend(group.groupNumber, friendNumber)
                }
            }
            true
        } else {
            false
        }
    }

    suspend fun joinByChatId(chatIdHex: String, selfName: String, password: String? = null): Int = withContext(Dispatchers.IO) {
        if (chatIdHex.length != 64) return@withContext -3
        if (groupRepository.exists(chatIdHex)) return@withContext -2

        val chatIdBytes: ByteArray
        try {
            chatIdBytes = chatIdHex.hexToBytes()
        } catch (e: Exception) {
            return@withContext -4
        }

        val groupNumber = tox.groupJoinDirect(
            chatIdBytes,
            selfName.toByteArray(),
            password?.toByteArray(),
        )

        if (groupNumber >= 0) {
            val chatId = chatIdBytes.bytesToHex().lowercase()

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

            // Connection timeout to prevent hanging in Connecting status forever
            scope.launch {
                delay(45000)
                val g = groupRepository.get(chatId).firstOrNull()
                if (g != null && !g.connected && connectionStatus(chatId) == GroupConnectionStatus.Connecting) {
                    Log.w("GroupManager", "Direct join connection timeout for $chatId")
                    setConnectionStatus(chatId, GroupConnectionStatus.Disconnected)
                }
            }

            val ourPeer = GroupPeer(
                groupChatId = chatId,
                peerId = selfPeerId,
                name = selfName,
                publicKey = tox.publicKey.string(),
                role = selfRole.name,
                isOurselves = true,
            )
            groupRepository.addPeer(ourPeer)
        }

        groupNumber
    }

}

