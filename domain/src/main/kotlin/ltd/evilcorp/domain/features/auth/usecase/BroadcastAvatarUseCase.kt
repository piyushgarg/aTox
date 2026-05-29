package ltd.evilcorp.domain.features.auth.usecase

import javax.inject.Inject
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.broadcastAvatar

/**
 * Use case to broadcast the local user's avatar to all online contacts.
 */
class BroadcastAvatarUseCase @Inject constructor(
    private val fileTransferManager: FileTransferManager,
) {
    suspend fun execute() {
        fileTransferManager.broadcastAvatar()
    }
}
