package ltd.evilcorp.atox.infrastructure.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ltd.evilcorp.atox.infrastructure.backup.google.GoogleDriveSyncWorker
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import ltd.evilcorp.domain.features.settings.model.UserSettings
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val HOURS_IN_DAY = 24L
private const val HOURS_IN_WEEK = 168L
private const val HOURS_IN_MONTH = 720L

class BackupWorkScheduler @Inject constructor(
    private val context: Context
) {
    fun scheduleBackups(settings: UserSettings) {
        val workManager = WorkManager.getInstance(context)

        if (!settings.automaticBackupEnabled) {
            workManager.cancelUniqueWork("GoogleDriveBackup")
            workManager.cancelUniqueWork("LocalBackup")
            return
        }

        val repeatIntervalHours = when (settings.backupFrequency) {
            BackupFrequency.Off -> return // Handled above, but just in case
            BackupFrequency.Daily -> HOURS_IN_DAY
            BackupFrequency.Weekly -> HOURS_IN_WEEK
            BackupFrequency.Monthly -> HOURS_IN_MONTH
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .apply {
                if (!settings.backupUseCellular) {
                    setRequiredNetworkType(NetworkType.UNMETERED)
                } else {
                    setRequiredNetworkType(NetworkType.CONNECTED)
                }
            }
            .build()

        if (settings.backupGoogleAccount.isNotEmpty()) {
            val googleRequest = PeriodicWorkRequestBuilder<GoogleDriveSyncWorker>(repeatIntervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(
                "GoogleDriveBackup",
                ExistingPeriodicWorkPolicy.UPDATE,
                googleRequest
            )
        } else {
            workManager.cancelUniqueWork("GoogleDriveBackup")
        }

        val localRequest = PeriodicWorkRequestBuilder<LocalSyncWorker>(repeatIntervalHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "LocalBackup",
            ExistingPeriodicWorkPolicy.UPDATE,
            localRequest
        )
    }
}
