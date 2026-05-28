package ltd.evilcorp.domain.features.auth.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.broadcastAvatar
import ltd.evilcorp.domain.features.auth.repository.IAvatarRepository
import javax.inject.Inject

class SaveAvatarUseCase @Inject constructor(
    private val avatarRepository: IAvatarRepository,
    private val fileTransferManager: FileTransferManager
) {
    suspend fun execute(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val success = avatarRepository.saveSelfAvatar(bytes)
        if (success) {
            fileTransferManager.broadcastAvatar()
        }
        success
    }
}
