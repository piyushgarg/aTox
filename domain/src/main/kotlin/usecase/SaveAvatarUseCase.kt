package ltd.evilcorp.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.repository.IAvatarStorage
import javax.inject.Inject

class SaveAvatarUseCase @Inject constructor(
    private val avatarStorage: IAvatarStorage,
    private val fileTransferManager: FileTransferManager
) {
    suspend fun execute(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val success = avatarStorage.saveSelfAvatar(bytes)
        if (success) {
            fileTransferManager.broadcastAvatar()
        }
        success
    }
}
