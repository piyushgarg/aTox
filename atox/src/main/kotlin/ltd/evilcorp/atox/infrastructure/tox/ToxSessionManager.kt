package ltd.evilcorp.atox.infrastructure.tox

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.save.ISaveManager
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.reset

/**
 * Manages the lifecycle of a Tox user session.
 * Coordinates loading/saving profiles, verifying users, resetting file transfer status,
 * and synchronizing contacts upon startup.
 */
@Singleton
class ToxSessionManager @Inject constructor(
    private val saveManager: ISaveManager,
    private val userManager: UserManager,
    private val fileTransferManager: FileTransferManager,
    private val startupSynchronizer: ToxStartupSynchronizer,
    private val scope: CoroutineScope,
) {
    fun loadSave(publicKey: PublicKey): ByteArray? {
        return saveManager.load(publicKey)
    }

    fun listSaves(): List<String> {
        return saveManager.list()
    }

    fun onToxStarted(publicKey: PublicKey) {
        startupSynchronizer.synchronizeAfterStart()
        fileTransferManager.reset()
        scope.launch {
            userManager.verifyExists(publicKey)
        }
    }
}
