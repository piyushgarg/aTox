package ltd.evilcorp.domain.features.backup.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.core.di.IoDispatcher
import ltd.evilcorp.domain.features.backup.ExportManager
import javax.inject.Inject

class ExportChatHistoryUseCase @Inject constructor(
    private val exportManager: ExportManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    @Suppress("RedundantSuspendModifier")
    suspend fun execute(publicKeyString: String): String = withContext(ioDispatcher) {
        exportManager.generateExportMessagesJString(publicKeyString)
    }
}

