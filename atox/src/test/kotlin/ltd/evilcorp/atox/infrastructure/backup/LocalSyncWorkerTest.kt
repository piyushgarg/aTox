package ltd.evilcorp.atox.infrastructure.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.backup.usecase.ExportBackupUseCase
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var exportBackupUseCase: ExportBackupUseCase
    private lateinit var userSettingsRepository: IUserSettingsRepository
    private lateinit var getSelfUserUseCase: GetSelfUserUseCase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        workerParams = mockk(relaxed = true)
        exportBackupUseCase = mockk()
        userSettingsRepository = mockk(relaxed = true)
        getSelfUserUseCase = mockk(relaxed = true)
    }

    @Test
    fun testDoWork_failure() = runBlocking {
        coEvery { exportBackupUseCase.execute(any()) } throws Exception("Export failed")

        val worker = LocalSyncWorker(context, workerParams, exportBackupUseCase, userSettingsRepository, getSelfUserUseCase)
        val result = worker.doWork()

        assertEquals(Result.retry(), result)
    }
}
