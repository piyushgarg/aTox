package ltd.evilcorp.atox.infrastructure.backup

import javax.inject.Inject
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.core.network.save.ISaveManager
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.core.network.ITox

class ToxCoreBackupDataProvider @Inject constructor(
    private val tox: ITox,
    private val saveManager: ISaveManager,
) : IBackupDataProvider {
    override val id: String = "tox_core"
    override val displayNameRes: Int = R.string.backup_module_tox_core
    override val descriptionRes: Int = R.string.backup_module_tox_core_description

    override suspend fun serialize(outputStream: java.io.OutputStream) {
        outputStream.write(tox.getSaveData())
    }

    override suspend fun deserialize(data: ByteArray) {
        saveManager.save(tox.publicKey, data)
    }
}
