package ltd.evilcorp.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.model.PublicKey
import javax.inject.Inject

class ManageFileTransferUseCase @Inject constructor(
    private val fileTransferManager: FileTransferManager
) {
    suspend fun accept(fileNumber: Int) = withContext(Dispatchers.IO) {
        fileTransferManager.accept(fileNumber)
    }

    suspend fun reject(fileNumber: Int) = withContext(Dispatchers.IO) {
        fileTransferManager.reject(fileNumber)
    }

    suspend fun delete(correlationId: Int) = withContext(Dispatchers.IO) {
        fileTransferManager.delete(correlationId)
    }

    suspend fun create(publicKey: PublicKey, fileUriString: String) = withContext(Dispatchers.IO) {
        fileTransferManager.create(publicKey, fileUriString)
    }
}
