package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.transfer.FileTransferManager
import javax.inject.Inject

/**
 * Use case to accept an incoming file transfer request.
 */
class AcceptGroupFileTransferUseCase @Inject constructor(
    private val fileTransferManager: FileTransferManager,
) {
    suspend fun execute(fileNumber: Int) {
        fileTransferManager.accept(fileNumber)
    }
}
