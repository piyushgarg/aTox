package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.transfer.FileTransferManager
import javax.inject.Inject

/**
 * Use case to reject an incoming file transfer request.
 */
class RejectGroupFileTransferUseCase @Inject constructor(
    private val fileTransferManager: FileTransferManager,
) {
    fun execute(fileNumber: Int) {
        fileTransferManager.reject(fileNumber)
    }
}
