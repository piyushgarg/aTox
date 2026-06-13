package ltd.evilcorp.domain.features.auth.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.core.di.IoDispatcher
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.broadcastAvatar
import ltd.evilcorp.domain.features.auth.repository.IAvatarRepository
import javax.inject.Inject

class SaveAvatarUseCase @Inject constructor(
    private val avatarRepository: IAvatarRepository,
    private val fileTransferManager: FileTransferManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun execute(bytes: ByteArray): Boolean = withContext(ioDispatcher) {
        val success = avatarRepository.saveSelfAvatar(bytes)
        if (success) {
            fileTransferManager.broadcastAvatar()
        }
        success
    }
}

