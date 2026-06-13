package ltd.evilcorp.atox.infrastructure.backup.google

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.backup.usecase.ExportBackupUseCase
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleDriveSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var exportBackupUseCase: ExportBackupUseCase
    private lateinit var userSettingsRepository: IUserSettingsRepository
    private lateinit var googleDriveBackupHelper: GoogleDriveBackupHelper
    private lateinit var getSelfUserUseCase: GetSelfUserUseCase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        workerParams = mockk(relaxed = true)
        exportBackupUseCase = mockk()
        userSettingsRepository = mockk(relaxed = true)
        googleDriveBackupHelper = mockk(relaxed = true)
        getSelfUserUseCase = mockk(relaxed = true)
    }

    @Test
    fun testDoWork_noAccount() = runBlocking {
        // Mock no account
        coEvery { googleDriveBackupHelper.getAccount() } returns null
        every { userSettingsRepository.settings } returns MutableStateFlow(UserSettings(backupGoogleAccount = ""))

        val worker = GoogleDriveSyncWorker(context, workerParams, exportBackupUseCase, userSettingsRepository, googleDriveBackupHelper, getSelfUserUseCase)
        val result = worker.doWork()

        assertEquals(Result.failure(), result)
    }

    @Test
    fun testDoWork_exportFailure() = runBlocking {
        // Mock account exists
        coEvery { googleDriveBackupHelper.getAccount() } returns mockk()
        every { userSettingsRepository.settings } returns MutableStateFlow(UserSettings(backupGoogleAccount = "test@example.com"))
        coEvery { exportBackupUseCase.execute(any()) } throws Exception("Export failed")

        val worker = GoogleDriveSyncWorker(context, workerParams, exportBackupUseCase, userSettingsRepository, googleDriveBackupHelper, getSelfUserUseCase)
        val result = worker.doWork()

        assertEquals(Result.retry(), result)
    }
}
