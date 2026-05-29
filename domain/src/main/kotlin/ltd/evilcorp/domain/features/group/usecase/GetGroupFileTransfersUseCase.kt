package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository
import javax.inject.Inject

/**
 * Use case to retrieve the active file transfers list stream for a specific chat.
 */
class GetGroupFileTransfersUseCase @Inject constructor(
    private val fileTransferRepository: IFileTransferRepository,
) {
    fun execute(chatId: String): Flow<List<FileTransfer>> {
        return fileTransferRepository.get(chatId)
    }
}
