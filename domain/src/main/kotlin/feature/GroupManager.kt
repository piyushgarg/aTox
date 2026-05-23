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
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.GroupRepository
import ltd.evilcorp.core.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.core.tox.enums.ToxMessageType
import ltd.evilcorp.domain.tox.Tox

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
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val chatManager: ChatManager,
    private val tox: Tox,
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

    fun connectionStatus(chatId: String): GroupConnectionStatus =
        _connectionStatuses.value[chatId] ?: GroupConnectionStatus.Disconnected

    fun setConnectionStatus(chatId: String, status: GroupConnectionStatus) {
        _connectionStatuses.value = _connectionStatuses.value + (chatId to status)
    }

    fun setPendingInvite(invite: GroupInvite?) {
        _pendingInvite.value = invite
    }

    private suspend fun reconnectWithRetry(chatId: String, groupNumber: Int, maxRetries: Int = 5) {
        for (attempt in 0 until maxRetries) {
            val ok = tox.groupReconnect(groupNumber)
            if (ok) return
            Log.w("GroupManager", "Reconnect attempt $attempt failed for group $chatId, retrying in ${(attempt + 1) * 2}s")
            delay(2000L * (attempt + 1))
        }
        Log.e("GroupManager", "All $maxRetries reconnect attempts failed for group $chatId")
        setConnectionStatus(chatId, GroupConnectionStatus.Disconnected)
    }

    fun reconnectAll() {
        scope.launch {
            val groups = groupRepository.getAll().firstOrNull() ?: return@launch
            for (group in groups) {
                groupRepository.setConnected(group.chatId, false)
                setConnectionStatus(group.chatId, GroupConnectionStatus.Reconnecting)
                if (group.groupNumber >= 0) {
                    reconnectWithRetry(group.chatId, group.groupNumber)
                }
            }
        }
    }

    fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {
        scope.launch {
            delay(3000)
            val g = groupRepository.get(chatId).firstOrNull()
            if (g == null || g.connected) return@launch
            setConnectionStatus(chatId, GroupConnectionStatus.Reconnecting)
            reconnectWithRetry(chatId, groupNumber)
        }
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

    fun joinGroup(
        friendNo: Int,
        inviteData: ByteArray,
        selfName: String,
        password: String? = null,
    ): Int {
        val groupNumber = tox.groupJoin(
            friendNo,
            inviteData,
            selfName.toByteArray(),
            password?.toByteArray(),
        )

        if (groupNumber >= 0) {
            val chatIdBytes = tox.groupGetChatId(groupNumber)
            val chatId = chatIdBytes?.toHexString() ?: "unknown_$groupNumber"

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

    fun leaveGroup(chatId: String) = scope.launch {
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

    fun getChatId(chatId: String): String? {
        var result: String? = null
        runBlocking {
            result = groupRepository.get(chatId).firstOrNull()?.chatId
        }
        return result
    }

    fun inviteFriend(chatId: String, friendPublicKey: String): Boolean {
        var result = false
        runBlocking {
            val group = groupRepository.get(chatId).firstOrNull()
            if (group != null && group.groupNumber >= 0) {
                val pk = PublicKey(friendPublicKey)
                val friendNumber = tox.getFriendNumber(pk)
                if (friendNumber >= 0) {
                    val contact = contactRepository.get(friendPublicKey).firstOrNull()
                    val isOnline = contact?.connectionStatus != ConnectionStatus.None
                    if (isOnline) {
                        result = tox.groupInviteSend(group.groupNumber, friendNumber)
                    } else {
                        val inviteText = "Join my group \"${group.name}\"!\nChat ID: ${group.chatId}"
                        chatManager.sendMessage(pk, inviteText, MessageType.Normal)
                        result = true
                    }
                }
            }
        }
        return result
    }

    fun joinByChatId(chatIdHex: String, selfName: String, password: String? = null): Int {
        if (chatIdHex.length != 64) return -3
        if (groupRepository.exists(chatIdHex)) return -2

        val chatIdBytes: ByteArray
        try {
            chatIdBytes = chatIdHex.hexToByteArray()
        } catch (e: Exception) {
            return -4
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

    private fun String.hexToByteArray(): ByteArray {
        val result = ByteArray(length / 2)
        for (i in indices step 2) {
            result[i / 2] = ((substring(i, i + 2).toInt(16)) and 0xFF).toByte()
        }
        return result
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
