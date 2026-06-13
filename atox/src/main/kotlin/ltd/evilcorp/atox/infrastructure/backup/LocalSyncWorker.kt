package ltd.evilcorp.atox.infrastructure.backup

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.backup.usecase.ExportBackupUseCase
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import java.io.File
import java.io.FileOutputStream

private const val KB_IN_MB = 1024L

@HiltWorker
class LocalSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val userSettingsRepository: IUserSettingsRepository,
    private val getSelfUserUseCase: GetSelfUserUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("LocalBackup", "Starting Local Backup...")
            
            val settings = userSettingsRepository.settings.value
            val selectedIds = setOf("tox_core", "chat_history", "contacts", "file_transfer")

            // Perform backup export
            val backupBytes = exportBackupUseCase.execute(selectedIds)
            
            // Save to Documents/aTox
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val atoxDir = File(documentsDir, "aTox")
            if (!atoxDir.exists()) {
                atoxDir.mkdirs()
            }
            
            val profileId = runCatching { getSelfUserUseCase.publicKey.string().take(8) }.getOrDefault("unknown")
            val filename = "atox_backup_${profileId}_${System.currentTimeMillis()}.zip"
            val backupFile = File(atoxDir, filename)
            
            FileOutputStream(backupFile).use { fos ->
                fos.write(backupBytes)
            }
            
            val sizeKb = backupFile.length() / KB_IN_MB
            
            // Save stats
            userSettingsRepository.updateLastLocalBackupTimeMs(System.currentTimeMillis())
            userSettingsRepository.updateLastLocalBackupSizeKb(sizeKb)
            
            Log.d("LocalBackup", "Local Backup completed successfully. Saved to ${backupFile.absolutePath} Size: $sizeKb KB")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("LocalBackup", "Local Backup failed", e)
            Result.retry()
        }
    }
}
