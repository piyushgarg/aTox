package ltd.evilcorp.domain.features.backup.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.domain.features.settings.ISettingsFileProcessor
import ltd.evilcorp.domain.core.network.bootstrap.BootstrapNodeJsonParser

class ImportBootstrapNodesUseCase @Inject constructor(
    private val fileProcessor: ISettingsFileProcessor,
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
