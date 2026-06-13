package ltd.evilcorp.atox.infrastructure.backup.google

import android.content.Context
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

@HiltWorker
class GoogleDriveSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val userSettingsRepository: IUserSettingsRepository,
    private val googleDriveBackupHelper: GoogleDriveBackupHelper,
    private val getSelfUserUseCase: GetSelfUserUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val account = googleDriveBackupHelper.getAccount()
            if (account == null) {
                Log.e("GoogleDrive", "No Google account signed in. Aborting backup.")
                return@withContext Result.failure()
            }

            Log.d("GoogleDrive", "Starting Google Drive Backup...")
            
            // Collect selected backup IDs based on settings (assuming all are exported for now)
            val settings = userSettingsRepository.settings.value
            val selectedIds = setOf("tox_core", "chat_history", "contacts", "file_transfer") // We can fetch from DB, or use default

            // Perform backup export
            val backupBytes = exportBackupUseCase.execute(selectedIds)
            
            val profileId = runCatching { getSelfUserUseCase.publicKey.string().take(8) }.getOrDefault("unknown")
            val filename = "atox_backup_${profileId}_${System.currentTimeMillis()}.zip"
            
            // Upload to Google Drive
            val sizeKb = googleDriveBackupHelper.uploadBackup(backupBytes, filename)
            
            // Save stats
            userSettingsRepository.updateLastGoogleBackupTimeMs(System.currentTimeMillis())
            userSettingsRepository.updateLastGoogleBackupSizeKb(sizeKb)
            
            Log.d("GoogleDrive", "Google Drive Backup completed successfully. Size: $sizeKb KB")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("GoogleDrive", "Google Drive Backup failed", e)
            Result.retry()
        }
    }
}
