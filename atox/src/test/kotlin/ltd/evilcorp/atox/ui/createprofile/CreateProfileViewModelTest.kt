package ltd.evilcorp.atox.ui.createprofile

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.atox.MainDispatcherRule
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.auth.usecase.ClearDatabaseUseCase
import ltd.evilcorp.domain.features.auth.usecase.CreateProfileUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.auth.usecase.VerifyProfileExistsUseCase
import ltd.evilcorp.domain.features.backup.usecase.GetBackupProviderDataUseCase
import ltd.evilcorp.domain.features.backup.usecase.ImportBackupUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ltd.evilcorp.domain.features.auth.usecase.ProfileRegistryUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CreateProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockBackupProcessor = mockk<ProfileBackupProcessor>(relaxed = true)
    private val mockClearDatabaseUseCase = mockk<ClearDatabaseUseCase>(relaxed = true)
    private val mockGetBackupProviderDataUseCase = mockk<GetBackupProviderDataUseCase>(relaxed = true)
    private val mockImportBackupUseCase = mockk<ImportBackupUseCase>(relaxed = true)
    private val mockCreateProfileUseCase = mockk<CreateProfileUseCase>(relaxed = true)
    private val mockGetSelfUserUseCase = mockk<GetSelfUserUseCase>(relaxed = true)
    private val mockVerifyProfileExistsUseCase = mockk<VerifyProfileExistsUseCase>(relaxed = true)
    private val mockToxStarter = mockk<ToxStarter>(relaxed = true)
    private val mockTox = mockk<ltd.evilcorp.domain.core.network.ITox>(relaxed = true)
    private val mockProfileRegistryUseCase = mockk<ProfileRegistryUseCase>(relaxed = true)

    @org.junit.Before
    fun setUp() {
        every { mockProfileRegistryUseCase.getActiveProfileId() } returns "default"
        every { mockProfileRegistryUseCase.getProfiles() } returns emptyList()
        every { mockTox.getName() } returns "Test User"
    }

    private fun createViewModel(): CreateProfileViewModel {
        return CreateProfileViewModel(
            backupProcessor = mockBackupProcessor,
            clearDatabaseUseCase = mockClearDatabaseUseCase,
            getBackupProviderDataUseCase = mockGetBackupProviderDataUseCase,
            importBackupUseCase = mockImportBackupUseCase,
            createProfileUseCase = mockCreateProfileUseCase,
            getSelfUserUseCase = mockGetSelfUserUseCase,
            verifyProfileExistsUseCase = mockVerifyProfileExistsUseCase,
            toxStarter = mockToxStarter,
            getCloudBackupsUseCase = mockk(relaxed = true),
            downloadCloudBackupUseCase = mockk(relaxed = true),
            tox = mockTox,
            profileRegistryUseCase = mockProfileRegistryUseCase,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun `createProfile success path`() = runTest {
        val viewModel = createViewModel()
        every { mockToxStarter.startTox(any(), any()) } returns ToxSaveStatus.Ok
        every { mockGetSelfUserUseCase.publicKey } returns PublicKey("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")

        viewModel.uiState.test {
            assertEquals(CreateProfileUiState.Idle, awaitItem())

            viewModel.createProfile("TestUser")

            assertEquals(CreateProfileUiState.Loading, awaitItem())
            assertEquals(CreateProfileUiState.Success, awaitItem())

            coVerify(exactly = 1) { mockCreateProfileUseCase.execute(match { it.name == "TestUser" }) }
        }
    }

    @Test
    fun `createProfile tox error path`() = runTest {
        val viewModel = createViewModel()
        every { mockToxStarter.startTox(any(), any()) } returns ToxSaveStatus.BadProxyHost

        viewModel.uiState.test {
            assertEquals(CreateProfileUiState.Idle, awaitItem())

            viewModel.createProfile("TestUser")

            assertEquals(CreateProfileUiState.Loading, awaitItem())
            assertEquals(CreateProfileUiState.Error(R.string.bad_host), awaitItem())
        }
    }

    @Test
    fun `restoreBackup success path`() = runTest {
        val viewModel = createViewModel()
        coEvery { mockBackupProcessor.readBackupBytes(any()) } returns byteArrayOf(1, 2, 3)
        coEvery { mockGetBackupProviderDataUseCase.execute(any(), any()) } returns byteArrayOf(4, 5, 6)
        every { mockToxStarter.startTox(any(), any()) } returns ToxSaveStatus.Ok
        every { mockGetSelfUserUseCase.publicKey } returns PublicKey("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")

        viewModel.uiState.test {
            assertEquals(CreateProfileUiState.Idle, awaitItem())

            viewModel.restoreBackup("file://test", "password")

            assertEquals(CreateProfileUiState.Loading, awaitItem())
            assertEquals(CreateProfileUiState.Success, awaitItem())

            coVerify(exactly = 1) { mockClearDatabaseUseCase.execute() }
            coVerify(exactly = 1) { mockImportBackupUseCase.execute(any(), any()) }
            coVerify(exactly = 1) { mockVerifyProfileExistsUseCase.execute(any()) }
        }
    }

    @Test
    fun `restoreBackup missing file path`() = runTest {
        val viewModel = createViewModel()
        coEvery { mockBackupProcessor.readBackupBytes(any()) } returns null

        viewModel.uiState.test {
            assertEquals(CreateProfileUiState.Idle, awaitItem())

            viewModel.restoreBackup("file://test", "password")

            assertEquals(CreateProfileUiState.Loading, awaitItem())
            assertEquals(CreateProfileUiState.Error(R.string.backup_import_failure), awaitItem())
        }
    }
}
