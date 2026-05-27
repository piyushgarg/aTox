package ltd.evilcorp.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.feature.ExportManager
import javax.inject.Inject

class ExportChatHistoryUseCase @Inject constructor(
    private val exportManager: ExportManager
) {
    suspend fun execute(publicKeyString: String): String = withContext(Dispatchers.IO) {
        exportManager.generateExportMessagesJString(publicKeyString)
    }
}
