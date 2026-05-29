// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.Log

@Singleton
class GroupMessagingService @Inject constructor(
    private val scope: CoroutineScope,
    private val groupRepository: IGroupRepository,
    private val messageRepository: IMessageRepository,
    private val contactRepository: IContactRepository,
    private val toxServices: GroupToxServices,
    private val sessionCoordinator: GroupSessionCoordinator,
) {
    private val sessionRegistry get() = sessionCoordinator.sessionRegistry
    private val tox get() = toxServices.tox
    private val toxProfile get() = toxServices.profile

    suspend fun sendMessage(chatId: String, message: String, type: MessageType = MessageType.Normal) = withContext(Dispatchers.IO) {
        val g = groupRepository.get(chatId).firstOrNull() ?: return@withContext
        val status = sessionRegistry.connectionStatuses.value[chatId] ?: GroupConnectionStatus.Disconnected
        val toxType = when (type) {
            MessageType.Normal -> ToxMessageType.NORMAL
            MessageType.Action -> ToxMessageType.ACTION
            else -> ToxMessageType.NORMAL
        }

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
                timestamp = System.currentTimeMillis(),
            )
            groupRepository.addMessage(groupMsg)
            if (msgId < 0) {
                Log.w("GroupMessagingService", "sendMessage failed for $chatId, queued for resend")
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
                timestamp = System.currentTimeMillis(),
            )
            groupRepository.addMessage(groupMsg)
        }
    }

    fun resendPendingMessages(chatId: String) {
        scope.launch {
            val g = groupRepository.get(chatId).firstOrNull() ?: return@launch
            val unsent = groupRepository.getUnsentMessages(chatId)
            if (unsent.isEmpty()) return@launch
            Log.i("GroupMessagingService", "Resending ${unsent.size} pending messages to $chatId")
            for (msg in unsent) {
                val toxType = when (msg.type) {
                    MessageType.Normal -> ToxMessageType.NORMAL
                    MessageType.Action -> ToxMessageType.ACTION
                    else -> ToxMessageType.NORMAL
                }
                val newId = tox.groupSendMessage(
                    g.groupNumber,
                    toxType,
                    msg.message.toByteArray(),
                )
                if (newId >= 0) {
                    groupRepository.setCorrelationId(msg.id, newId)
                } else {
                    Log.w("GroupMessagingService", "Failed to resend message ${msg.id} to $chatId")
                }
            }
        }
    }

    suspend fun inviteFriend(chatId: String, friendPublicKey: String): Boolean = withContext(Dispatchers.IO) {
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
                    timestamp = System.currentTimeMillis()
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
}
