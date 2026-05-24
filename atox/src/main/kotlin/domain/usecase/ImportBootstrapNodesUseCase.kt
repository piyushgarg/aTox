package ltd.evilcorp.atox.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.atox.ui.settings.SettingsFileProcessor
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeJsonParser

class ImportBootstrapNodesUseCase @Inject constructor(
    private val fileProcessor: SettingsFileProcessor,
    private val nodeParser: BootstrapNodeJsonParser,
) {
    suspend fun validate(uriString: String): Boolean = withContext(Dispatchers.IO) {
        val bytes = fileProcessor.readBytes(uriString) ?: return@withContext false
        return@withContext nodeParser.parse(bytes.decodeToString()).isNotEmpty()
    }

    suspend fun import(uriString: String): Boolean = withContext(Dispatchers.IO) {
        val bytes = fileProcessor.readBytes(uriString) ?: return@withContext false
        return@withContext fileProcessor.saveUserNodesJson(bytes)
    }
}
