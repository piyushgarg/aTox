package ltd.evilcorp.atox.infrastructure.backup

import javax.inject.Inject
import ltd.evilcorp.atox.R
import ltd.evilcorp.core.tox.save.SaveManager
import ltd.evilcorp.domain.backup.BackupDataProvider
import ltd.evilcorp.domain.tox.ITox

class ToxCoreBackupDataProvider @Inject constructor(
    private val tox: ITox,
    private val saveManager: SaveManager,
) : BackupDataProvider {
    override val id: String = "tox_core"
    override val displayNameRes: Int = R.string.backup_module_tox_core
    override val descriptionRes: Int = R.string.backup_module_tox_core_description

    override fun serialize(): ByteArray = tox.getSaveData()

    override fun deserialize(data: ByteArray) {
        saveManager.save(tox.publicKey, data)
    }
}
