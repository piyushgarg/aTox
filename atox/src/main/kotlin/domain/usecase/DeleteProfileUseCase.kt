package ltd.evilcorp.atox.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.tox.save.SaveManager
import ltd.evilcorp.domain.tox.Tox

class DeleteProfileUseCase @Inject constructor(
    private val tox: Tox,
    private val toxStarter: ToxStarter,
    private val saveManager: SaveManager,
    private val database: Database,
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        val pk = tox.publicKey
        toxStarter.stopTox()
        saveManager.delete(pk)
        saveManager.list().forEach {
            try {
                saveManager.delete(PublicKey(it))
            } catch (e: Exception) {
                // Ignore
            }
        }
        database.clearAllTables()
    }
}
