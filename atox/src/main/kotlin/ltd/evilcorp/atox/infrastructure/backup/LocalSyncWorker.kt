package ltd.evilcorp.atox.infrastructure.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
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

@Suppress("unused")
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
            Log.d("LocalBackup", "Got settings, localBackupDirectoryUri: ${settings.localBackupDirectoryUri}")

            val selectedIds = setOf("tox_core", "chat_history", "contacts", "file_transfer")

            // Perform backup export
            Log.d("LocalBackup", "Exporting backup...")
            val backupBytes = exportBackupUseCase.execute(selectedIds)
            Log.d("LocalBackup", "Backup exported, size: ${backupBytes.size} bytes")

            val profileId = runCatching { getSelfUserUseCase.publicKey.string().take(8) }.getOrDefault("unknown")
            val filename = "atox_backup_${profileId}_${System.currentTimeMillis()}.zip"
            Log.d("LocalBackup", "Filename: $filename")

            val backupFileSize = if (settings.localBackupDirectoryUri.isNotBlank()) {
                Log.d("LocalBackup", "Using custom directory")
                // Use user-selected directory
                val uri = Uri.parse(settings.localBackupDirectoryUri)
                val dir = DocumentFile.fromTreeUri(context, uri)
                if (dir != null && dir.exists() && dir.isDirectory) {
                    val file = dir.createFile("application/zip", filename)
                    if (file != null) {
                        context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                            outputStream.write(backupBytes)
                        }
                        Log.d("LocalBackup", "File written to custom directory")
                        file.length()
                    } else {
                        throw Exception("Failed to create backup file in selected directory")
                    }
                } else {
                    throw Exception("Selected backup directory is not accessible")
                }
            } else {
                Log.d("LocalBackup", "Using default Downloads/aTox directory")
                // Use default Downloads/aTox
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val atoxDir = File(downloadsDir, "aTox")
                if (!atoxDir.exists()) {
                    atoxDir.mkdirs()
                }

                val backupFile = File(atoxDir, filename)
                FileOutputStream(backupFile).use { fos ->
                    fos.write(backupBytes)
                }
                Log.d("LocalBackup", "File written to: ${backupFile.absolutePath}")
                backupFile.length()
            }

            val sizeKb = backupFileSize / KB_IN_MB

            // Save stats
            userSettingsRepository.updateLastLocalBackupTimeMs(System.currentTimeMillis())
            userSettingsRepository.updateLastLocalBackupSizeKb(sizeKb)

            Log.d("LocalBackup", "Local Backup completed successfully. Size: $sizeKb KB")

            Result.success()
        } catch (e: Exception) {
            Log.e("LocalBackup", "Local Backup failed: ${e.message}", e)
            Result.failure()
        }
    }
}
