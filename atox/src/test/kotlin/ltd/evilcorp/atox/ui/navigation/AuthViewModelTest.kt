package ltd.evilcorp.atox.ui.navigation

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import ltd.evilcorp.atox.MainDispatcherRule
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.settings.usecase.GetToxRunningStateUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ltd.evilcorp.domain.features.auth.usecase.GetSelfAvatarUseCase
import ltd.evilcorp.domain.features.auth.usecase.ProfileRegistryUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfNameUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockGetSelfUserUseCase = mockk<GetSelfUserUseCase>(relaxed = true)
    private val mockGetToxRunningStateUseCase = mockk<GetToxRunningStateUseCase>(relaxed = true)
    private val mockToxStarter = mockk<ToxStarter>(relaxed = true)
    private val mockGetSelfAvatarUseCase = mockk<GetSelfAvatarUseCase>(relaxed = true)
    private val mockProfileRegistryUseCase = mockk<ProfileRegistryUseCase>(relaxed = true)
    private val mockGetSelfNameUseCase = mockk<GetSelfNameUseCase>(relaxed = true)

    @org.junit.Before
    fun setUp() {
        every { mockProfileRegistryUseCase.getActiveProfileId() } returns "default"
        every { mockProfileRegistryUseCase.getProfiles() } returns emptyList()
        
        val mockFile = mockk<java.io.File>(relaxed = true)
        every { mockFile.exists() } returns false
        every { mockGetSelfAvatarUseCase.execute() } returns mockFile
        
        every { mockGetSelfNameUseCase.execute() } returns "Test User"
    }

    private fun createViewModel(
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = mainDispatcherRule.testDispatcher
    ): AuthViewModel {
        return AuthViewModel(
            getSelfUserUseCase = mockGetSelfUserUseCase,
            getToxRunningStateUseCase = mockGetToxRunningStateUseCase,
            toxStarter = mockToxStarter,
            getSelfAvatarUseCase = mockGetSelfAvatarUseCase,
            profileRegistryUseCase = mockProfileRegistryUseCase,
            getSelfNameUseCase = mockGetSelfNameUseCase,
            ioDispatcher = ioDispatcher
        )
    }

    @Test
    fun `loadToxAsync returns Success immediately if already running`() = runBlocking {
        every { mockGetToxRunningStateUseCase.execute() } returns true
        val viewModel = createViewModel()

        assertEquals(LaunchUiState.Loading, viewModel.launchState.value)
        viewModel.loadToxAsync(null)
        assertEquals(LaunchUiState.Success(ToxSaveStatus.Ok), viewModel.launchState.value)
    }

    @Test
    fun `loadToxAsync calls starter and returns status`() = runBlocking {
        every { mockGetToxRunningStateUseCase.execute() } returns false
        every { mockToxStarter.tryLoadTox("pass") } returns ToxSaveStatus.SaveNotFound
        val viewModel = createViewModel()

        assertEquals(LaunchUiState.Loading, viewModel.launchState.value)
        viewModel.loadToxAsync("pass")
        println("Launch state is: ${viewModel.launchState.value}")
        assertEquals(LaunchUiState.Success(ToxSaveStatus.SaveNotFound), viewModel.launchState.value)
    }

    @Test
    fun `loadToxAsync timeout sets state to Timeout`() = runTest {
        every { mockGetToxRunningStateUseCase.execute() } returns false
        coEvery { mockToxStarter.tryLoadTox(any()) } coAnswers {
            delay(15_000L) // Wait longer than LOAD_TIMEOUT_MS (10_000L)
            ToxSaveStatus.Ok
        }
        val viewModel = createViewModel(ioDispatcher = kotlinx.coroutines.Dispatchers.IO)

        viewModel.launchState.test {
            assertEquals(LaunchUiState.Loading, awaitItem())
            
            viewModel.loadToxAsync("pass")
            
            assertEquals(LaunchUiState.Timeout, awaitItem())
        }
    }

    @Test
    fun `unlockProfileAsync success`() = runBlocking {
        every { mockToxStarter.tryLoadTox(any()) } returns ToxSaveStatus.Ok
        val viewModel = createViewModel()

        viewModel.unlockState.test {
            assertEquals(UnlockUiState.Idle, awaitItem())

            val result = viewModel.unlockProfileAsync("password")

            assertEquals(UnlockUiState.Loading, awaitItem())
            assertEquals(UnlockUiState.Success, awaitItem())
            assertTrue(result)
        }
    }

    @Test
    fun `unlockProfileAsync failure`() = runBlocking {
        every { mockToxStarter.tryLoadTox("wrong") } returns ToxSaveStatus.Encrypted
        val viewModel = createViewModel()

        viewModel.unlockState.test {
            assertEquals(UnlockUiState.Idle, awaitItem())

            val result = viewModel.unlockProfileAsync("wrong")

            assertEquals(UnlockUiState.Loading, awaitItem())
            assertEquals(UnlockUiState.Error, awaitItem())
            assertFalse(result)
        }
    }

    @Test
    fun `clearUnlockError resets error state`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.unlockState.value = UnlockUiState.Error

        viewModel.clearUnlockError()

        assertEquals(UnlockUiState.Idle, viewModel.unlockState.value)
    }
}
