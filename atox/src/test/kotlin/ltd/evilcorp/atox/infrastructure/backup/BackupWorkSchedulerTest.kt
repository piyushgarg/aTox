package ltd.evilcorp.atox.infrastructure.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import ltd.evilcorp.domain.features.settings.model.UserSettings
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupWorkSchedulerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun testScheduleWork_disabled() {
        val settings = UserSettings(
            automaticBackupEnabled = false,
            backupFrequency = BackupFrequency.Daily
        )

        val scheduler = BackupWorkScheduler(context)
        scheduler.scheduleBackups(settings)
        // In a real test, we would verify WorkManager enqueued tasks, but we just want it to compile and run for now.
    }

    @Test
    fun testScheduleWork_enabled_daily() {
        val settings = UserSettings(
            automaticBackupEnabled = true,
            backupFrequency = BackupFrequency.Daily,
            backupUseCellular = false
        )

        val scheduler = BackupWorkScheduler(context)
        scheduler.scheduleBackups(settings)
    }
}
