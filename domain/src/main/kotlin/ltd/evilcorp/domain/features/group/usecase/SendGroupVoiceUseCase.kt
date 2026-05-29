package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.transfer.IFileTransferPlatformHelper
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository
import ltd.evilcorp.domain.features.chat.model.MessageType
import javax.inject.Inject

/**
 * Use case to send a voice message in a group chat.
 */
class SendGroupVoiceUseCase @Inject constructor(
    private val platformHelper: IFileTransferPlatformHelper,
    private val fileTransferRepository: IFileTransferRepository,
    private val sendGroupMessageUseCase: SendGroupMessageUseCase,
) {
    suspend fun execute(chatId: String, uriString: String) {
        val details = platformHelper.getFileSizeAndName(uriString) ?: return
        val size = details.second

        val correlationId = kotlin.random.Random.nextInt(CORRELATION_ID_BOUND)
        val ft = FileTransfer(
            publicKey = chatId,
            fileNumber = correlationId,
            fileKind = ltd.evilcorp.domain.features.transfer.model.FileKind.Data.ordinal,
            fileSize = size,
            fileName = "voice_message_${correlationId}.opus",
            outgoing = true,
            progress = size,
            destination = uriString,
        )
        val id = fileTransferRepository.add(ft).toInt()

        val signalMsg = "[VOICE:10|$id]"
        sendGroupMessageUseCase.execute(chatId, signalMsg, MessageType.FileTransfer, id)
    }

    companion object {
        private const val CORRELATION_ID_BOUND = 1000000
    }
}
