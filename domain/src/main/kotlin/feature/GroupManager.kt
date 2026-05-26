package ltd.evilcorp.domain.feature

import android.util.Log
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
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.GroupPeer
import ltd.evilcorp.core.model.GroupPrivacyState
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.domain.repository.IContactRepository
import ltd.evilcorp.domain.repository.IGroupRepository
import ltd.evilcorp.domain.repository.IMessageRepository
import ltd.evilcorp.core.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.core.tox.enums.ToxMessageType
import ltd.evilcorp.domain.tox.ITox

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

    private val reconnectJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    fun cancelReconnect(chatId: String) {
        reconnectJobs.remove(chatId)?.cancel()
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
            
            // 1. Проверяем и обновляем имя группы, если оно еще не прилетело по сети
            if (group.name.isEmpty() || group.name == "Unknown Group" || group.name.startsWith("unknown_")) {
                val groupNameBytes = tox.groupGetName(group.groupNumber)
                val groupName = groupNameBytes?.decodeToString()
                if (!groupName.isNullOrBlank() && groupName != "Unknown Group") {
                    groupRepository.setName(chatId, groupName)
                }
            }

            // 2. Проверяем и обновляем пустые publicKey у участников группы для загрузки аватарок
            val peers = groupRepository.getPeers(chatId).firstOrNull() ?: emptyList()
            peers.forEach { peer ->
                if (peer.publicKey.isEmpty() && peer.peerId >= 0 && !peer.isOurselves) {
                    val peerKeyBytes = tox.groupPeerGetPublicKey(group.groupNumber, peer.peerId)
                    val peerKey = peerKeyBytes?.toHexString()?.uppercase() ?: ""
                    if (peerKey.isNotEmpty()) {
                        val updatedPeer = peer.copy(publicKey = peerKey)
                        groupRepository.addPeer(updatedPeer)
                        Log.i("GroupManager", "Synchronously updated empty publicKey for peer ${peer.name} (${peer.peerId}) -> $peerKey")
                    }
                }
            }
        }
    }

    private suspend fun reconnectWithRetry(chatId: String, groupNumber: Int, maxRetries: Int = 999999) {
        for (attempt in 0 until maxRetries) {
            val currentStatus = connectionStatus(chatId)
            // Stop retrying if the group has successfully connected, or reconnect was aborted (e.g. user left the group)
            if (currentStatus == GroupConnectionStatus.Connected || (currentStatus == GroupConnectionStatus.Disconnected && attempt > 0)) {
                Log.d("GroupManager", "reconnectWithRetry for group $chatId stopped: status is $currentStatus")
                return
            }

            val ok = tox.groupReconnect(groupNumber)
            Log.d("GroupManager", "Reconnect attempt $attempt for group $chatId returned: $ok")
            if (ok) {
                setConnectionStatus(chatId, GroupConnectionStatus.Connecting)
            }
            
            // Адаптивная задержка (Exponential Back-off) для экономии батареи и сети:
            // - первые 10 попыток: каждые 5 секунд (быстрое переподключение)
            // - следующие 20 попыток: каждые 15 секунд
            // - все последующие попытки: каждые 30 секунд
            val delayMs = when {
                attempt < 10 -> 5000L
                attempt < 30 -> 15000L
                else -> 30000L
            }
            delay(delayMs)
        }
        
        val finalStatus = connectionStatus(chatId)
        if (finalStatus == GroupConnectionStatus.Reconnecting || finalStatus == GroupConnectionStatus.Connecting) {
            Log.e("GroupManager", "All $maxRetries reconnect attempts failed for group $chatId")
            setConnectionStatus(chatId, GroupConnectionStatus.Disconnected)
        }
    }

    fun reconnectAll() {
        scope.launch {
            val groups = groupRepository.getAll().firstOrNull() ?: return@launch
            Log.d("GroupManager", "reconnectAll found ${groups.size} groups in database")
            for (group in groups) {
                val currentStatus = connectionStatus(group.chatId)
                Log.d("GroupManager", "Group ${group.chatId} database status connected: ${group.connected}, current status state: $currentStatus, groupNumber: ${group.groupNumber}")
                
                if (currentStatus == GroupConnectionStatus.Connected) {
                    continue
                }
                
                reconnectJobs[group.chatId]?.cancel()
                
                groupRepository.setConnected(group.chatId, false)
                setConnectionStatus(group.chatId, GroupConnectionStatus.Reconnecting)
                if (group.groupNumber >= 0) {
                    Log.d("GroupManager", "Launching reconnectWithRetry for group ${group.chatId}")
                    val job = scope.launch {
                        reconnectWithRetry(group.chatId, group.groupNumber)
                    }
                    reconnectJobs[group.chatId] = job
                } else {
                    Log.w("GroupManager", "Group ${group.chatId} has invalid groupNumber ${group.groupNumber}, setting Disconnected")
                    setConnectionStatus(group.chatId, GroupConnectionStatus.Disconnected)
                }
            }
        }
    }

    fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {
        reconnectJobs[chatId]?.cancel()
        val job = scope.launch {
            delay(3000)
            val g = groupRepository.get(chatId).firstOrNull()
            if (g == null || g.connected) return@launch
            setConnectionStatus(chatId, GroupConnectionStatus.Reconnecting)
            reconnectWithRetry(chatId, groupNumber)
        }
        reconnectJobs[chatId] = job
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

    fun acceptInvite() {
        val invite = _pendingInvite.value ?: return
        _pendingInvite.value = null
        scope.launch {
            withContext(Dispatchers.IO) {
                val selfName = tox.getName()
                joinGroup(invite.friendNo, invite.inviteData, selfName)
            }
        }
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
            val chatId = chatIdBytes?.toHexString() ?: "unknown_$groupNumber"
            android.util.Log.i("GroupManager", "Joined group number $groupNumber, chatId = $chatId (attempts = $attempts)")

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
            inviteDataHex.hexToByteArray()
        } catch (e: Exception) {
            return -4 // Invalid hex format
        }

        return joinGroup(friendNo, inviteData, selfName, password)
    }

    fun leaveGroup(chatId: String) = scope.launch {
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

    fun sendMessage(chatId: String, message: String, type: MessageType = MessageType.Normal) = scope.launch {
        val g = groupRepository.get(chatId).firstOrNull() ?: return@launch
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

    fun setTopic(chatId: String, topic: String) = scope.launch {
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

    fun clearHistory(chatId: String) = scope.launch {
        groupRepository.deleteMessages(chatId)
        groupRepository.setLastMessage(chatId, 0)
    }

    fun deleteMessage(id: Long) = scope.launch {
        groupRepository.deleteMessage(id)
    }

    fun setDraft(chatId: String, draft: String) = scope.launch {
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
            
            // Всегда отображаем красивую интерактивную карточку-приглашение в чате у себя (у отправителя)
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
            chatIdBytes = chatIdHex.hexToByteArray()
        } catch (e: Exception) {
            return@withContext -4
        }

        val groupNumber = tox.groupJoinDirect(
            chatIdBytes,
            selfName.toByteArray(),
            password?.toByteArray(),
        )

        if (groupNumber >= 0) {
            val chatId = chatIdBytes.toHexString()

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

    private fun String.hexToByteArray(): ByteArray {
        val result = ByteArray(length / 2)
        for (i in indices step 2) {
            result[i / 2] = ((substring(i, i + 2).toInt(16)) and 0xFF).toByte()
        }
        return result
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
