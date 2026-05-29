package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.transfer.FileTransferManager
import javax.inject.Inject

/**
 * Use case to cancel or delete an active file transfer session.
 */
class CancelGroupFileTransferUseCase @Inject constructor(
    private val fileTransferManager: FileTransferManager,
) {
    suspend fun execute(correlationId: Int) {
        fileTransferManager.delete(correlationId)
    }
}
