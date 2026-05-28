@file:Suppress("MagicNumber")
package ltd.evilcorp.domain.features.group
import ltd.evilcorp.domain.core.network.Log

import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.hexToBytes
import ltd.evilcorp.domain.core.network.bytesToHex

suspend fun GroupManager.joinGroupWithBytes(
    friendPublicKey: String,
    inviteDataHex: String,
    selfName: String,
    password: String? = null,
): Int {
    val pk = PublicKey(friendPublicKey)
    val friendNo = toxProfile.getFriendNumber(pk)
    if (friendNo < 0) return -5

    val inviteData = try {
        inviteDataHex.hexToBytes()
    } catch (e: Exception) {
        return -4
    }

    return joinGroup(friendNo, inviteData, selfName, password)
}

suspend fun GroupManager.leaveGroup(chatId: String) = withContext(Dispatchers.IO) {
    cancelReconnect(chatId)
    val g = groupRepository.get(chatId).firstOrNull()
    g?.let {
        if (it.groupNumber >= 0) {
            tox.groupLeave(it.groupNumber)
        }
        groupRepository.deleteAllPeers(it.chatId)
        groupRepository.deleteByChatId(it.chatId)
        removeConnectionStatus(it.chatId)
    }
}

suspend fun GroupManager.sendMessage(chatId: String, message: String, type: MessageType = MessageType.Normal) = withContext(Dispatchers.IO) {
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
            senderName = toxProfile.getName(),
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
            senderName = toxProfile.getName(),
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

suspend fun GroupManager.inviteFriend(chatId: String, friendPublicKey: String): Boolean = withContext(Dispatchers.IO) {
    val group = groupRepository.get(chatId).firstOrNull()
    if (group != null && group.groupNumber >= 0) {
        val pk = PublicKey(friendPublicKey)
        val friendNumber = toxProfile.getFriendNumber(pk)
        
        val inviteText = "[GROUP_INVITE:${group.name}|${group.chatId}]"
        messageRepository.add(
            Message(
                publicKey = friendPublicKey.lowercase(),
                message = inviteText,
                sender = Sender.Sent,
                type = MessageType.Normal,
                correlationId = 0,
                timestamp = Date().time
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

suspend fun GroupManager.joinByChatId(chatIdHex: String, selfName: String, password: String? = null): Int = withContext(Dispatchers.IO) {
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
            publicKey = toxProfile.publicKey.string(),
            role = selfRole.name,
            isOurselves = true,
        )
        groupRepository.addPeer(ourPeer)
    }

    groupNumber
}

suspend fun GroupManager.setTopic(chatId: String, topic: String) = withContext(Dispatchers.IO) {
    val g = groupRepository.get(chatId).firstOrNull()
    g?.let {
        if (it.groupNumber >= 0) {
            tox.groupSetTopic(it.groupNumber, topic.toByteArray())
            groupRepository.setTopic(chatId, topic)
        }
    }
}

fun GroupManager.messagesFor(chatId: String): Flow<List<GroupMessage>> = groupRepository.getMessages(chatId)

fun GroupManager.getPeers(chatId: String): Flow<List<GroupPeer>> = groupRepository.getPeers(chatId)

suspend fun GroupManager.clearHistory(chatId: String) = withContext(Dispatchers.IO) {
    groupRepository.deleteMessages(chatId)
    groupRepository.setLastMessage(chatId, 0)
}

suspend fun GroupManager.deleteMessage(id: Long) = withContext(Dispatchers.IO) {
    groupRepository.deleteMessage(id)
}

suspend fun GroupManager.setDraft(chatId: String, draft: String) = withContext(Dispatchers.IO) {
    groupRepository.setDraftMessage(chatId, draft)
}

suspend fun GroupManager.getChatId(chatId: String): String? = withContext(Dispatchers.IO) {
    groupRepository.get(chatId).firstOrNull()?.chatId
}

suspend fun GroupManager.getChatIdByGroupNumber(groupNumber: Int): String? = withContext(Dispatchers.IO) {
    groupRepository.findChatIdByGroupNumber(groupNumber)
}
