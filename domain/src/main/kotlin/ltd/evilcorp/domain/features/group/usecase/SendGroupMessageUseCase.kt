package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import ltd.evilcorp.domain.features.group.GroupMessagingService
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.domain.features.chat.model.MessageType
import javax.inject.Inject

/**
 * Use case to send a message in a group chat, handling file transfer metadata binding if needed.
 */
class SendGroupMessageUseCase @Inject constructor(
    private val groupMessagingService: GroupMessagingService,
    private val groupRepository: IGroupRepository,
) {
    suspend fun execute(chatId: String, message: String, type: MessageType = MessageType.Normal, correlationId: Int = -1) {
        if (message.trim().isEmpty()) return
        groupMessagingService.sendMessage(chatId, message, type)
        if (type == MessageType.FileTransfer && correlationId != -1) {
            delay(FILE_TRANSFER_SIGNAL_DELAY_MS) // FILE_TRANSFER_SIGNAL_DELAY_MS
            groupRepository.getMessages(chatId).take(1).collect { list ->
                val lastMsg = list.lastOrNull { it.message == message }
                if (lastMsg != null) {
                    lastMsg.correlationId = correlationId
                    groupRepository.addMessage(lastMsg)
                }
            }
        }
    }

    companion object {
        private const val FILE_TRANSFER_SIGNAL_DELAY_MS = 150L
    }
}
