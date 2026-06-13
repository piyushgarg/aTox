// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import ltd.evilcorp.domain.core.di.IoDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.Log

@Singleton
class GroupMessagingService @Inject constructor(
    private val scope: CoroutineScope,
    private val repositories: GroupDataRepositories,
    private val toxServices: GroupToxServices,
    private val sessionCoordinator: GroupSessionCoordinator,
    private val groupEventBus: GroupEventBus,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val sessionRegistry get() = sessionCoordinator.sessionRegistry
    private val tox get() = toxServices.tox
    private val toxProfile get() = toxServices.profile

    suspend fun sendMessage(chatId: String, message: String, type: MessageType = MessageType.Normal) = withContext(ioDispatcher) {
        val g = repositories.group.get(chatId).firstOrNull() ?: return@withContext
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
            Log.i("GroupMessagingService", "Message submitted to Toxcore for $chatId, msgId=$msgId")
            val groupMsg = GroupMessage(
                groupChatId = chatId,
                peerId = g.selfPeerId,
                senderName = toxProfile.getName(),
                message = message,
                sender = Sender.Sent,
                type = type,
                correlationId = msgId,
                timestamp = System.currentTimeMillis(),
            )
            repositories.group.addMessage(groupMsg)
            if (msgId == -1) {
                Log.w("GroupMessagingService", "sendMessage failed for $chatId, queued for resend")
            } else {
                groupEventBus.emit(GroupDomainEvent.LocalMessageSent(chatId))
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
            repositories.group.addMessage(groupMsg)
        }
    }

    fun resendPendingMessages(chatId: String) {
        scope.launch {
            Log.i("GroupMessagingService", "resendPendingMessages called for $chatId")
            val g = repositories.group.getDirect(chatId)
            if (g == null) {
                Log.i("GroupMessagingService", "resendPendingMessages: group is null!")
                return@launch
            }
            val unsent = repositories.group.getUnsentMessages(chatId)
            if (unsent.isEmpty()) {
                Log.i("GroupMessagingService", "resendPendingMessages: unsent is empty!")
                return@launch
            }
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
                if (newId != -1) {
                    repositories.group.setCorrelationId(msg.id, newId)
                    groupEventBus.emit(GroupDomainEvent.LocalMessageSent(chatId))
                } else {
                    Log.w("GroupMessagingService", "Failed to resend message ${msg.id} to $chatId")
                }
            }
        }
    }

    suspend fun inviteFriend(chatId: String, friendPublicKey: String): Boolean = withContext(ioDispatcher) {
        val group = repositories.group.get(chatId).firstOrNull()
        if (group != null && group.groupNumber >= 0) {
            val pk = PublicKey(friendPublicKey)
            val friendNumber = toxProfile.getFriendNumber(pk)
            
            val inviteText = "[GROUP_INVITE:${group.name}|${group.chatId}]"
            repositories.message.add(
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
                val contact = repositories.contact.get(friendPublicKey).firstOrNull()
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
