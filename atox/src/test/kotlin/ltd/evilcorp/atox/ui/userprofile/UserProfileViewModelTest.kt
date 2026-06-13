@file:Suppress("MaxLineLength")
package ltd.evilcorp.atox.ui.userprofile

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.atox.MainDispatcherRule
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.auth.usecase.BroadcastAvatarUseCase
import ltd.evilcorp.domain.features.auth.usecase.DeleteProfileUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfAvatarUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.auth.usecase.ProfileAction
import ltd.evilcorp.domain.features.auth.usecase.SaveAvatarUseCase
import ltd.evilcorp.domain.features.auth.usecase.UpdateUserProfileUseCase
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import ltd.evilcorp.domain.features.auth.usecase.ProfileRegistryUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockGetSelfUserUseCase = mockk<GetSelfUserUseCase>(relaxed = true)
    private val mockUpdateUserProfileUseCase = mockk<UpdateUserProfileUseCase>(relaxed = true)
    private val mockBroadcastAvatarUseCase = mockk<BroadcastAvatarUseCase>(relaxed = true)
    private val mockGetSelfAvatarUseCase = mockk<GetSelfAvatarUseCase>(relaxed = true)
    private val mockSaveAvatarUseCase = mockk<SaveAvatarUseCase>(relaxed = true)
    private val mockDeleteProfileUseCase = mockk<DeleteProfileUseCase>(relaxed = true)

    private val createdViewModels = mutableListOf<UserProfileViewModel>()
    private val mockProfileRegistryUseCase = mockk<ProfileRegistryUseCase>(relaxed = true)

    @org.junit.Before
    fun setUp() {
        every { mockProfileRegistryUseCase.getActiveProfileId() } returns "default"
        every { mockProfileRegistryUseCase.getProfiles() } returns emptyList()
    }

    private fun createViewModel(): UserProfileViewModel {
        val vm = UserProfileViewModel(
            getSelfUserUseCase = mockGetSelfUserUseCase,
            updateUserProfileUseCase = mockUpdateUserProfileUseCase,
            broadcastAvatarUseCase = mockBroadcastAvatarUseCase,
            getSelfAvatarUseCase = mockGetSelfAvatarUseCase,
            saveAvatarUseCase = mockSaveAvatarUseCase,
            deleteProfileUseCase = mockDeleteProfileUseCase,
            profileRegistryUseCase = mockProfileRegistryUseCase,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
        createdViewModels.add(vm)
        return vm
    }

    @org.junit.After
    fun tearDown() {
        createdViewModels.forEach { it.clearForTest() }
        createdViewModels.clear()
    }

    @Test
    fun `init loads user data and avatar`() = runTest {
        val testUser = User(publicKey = "PK", name = "Satoshi")
        val userFlow = MutableStateFlow<User?>(testUser)
        
        every { mockGetSelfUserUseCase.publicKey } returns PublicKey("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        every { mockGetSelfUserUseCase.toxId } returns ToxID("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        every { mockGetSelfUserUseCase.execute() } returns userFlow

        val mockFile = mockk<File>(relaxed = true)
        every { mockFile.exists() } returns false // To skip bitmap decoding which needs android context
        every { mockGetSelfAvatarUseCase.execute() } returns mockFile

        val viewModel = createViewModel()

        viewModel.user.test {
            assertEquals(testUser, awaitItem())
        }

        assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", viewModel.publicKey.string())
        assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", viewModel.toxId.string())
    }

    @Test
    fun `setName is debounced and triggers usecase`() = runTest {
        val userFlow = MutableStateFlow<User?>(null)
        every { mockGetSelfUserUseCase.execute() } returns userFlow
        val viewModel = createViewModel()

        viewModel.setName("S")
        viewModel.setName("Sa")
        viewModel.setName("Satoshi")
        
        // Before 800ms, use case shouldn't be called
        advanceTimeBy(400)
        coVerify(exactly = 0) { mockUpdateUserProfileUseCase.execute(any()) }
        
        // After 800ms debounce
        advanceTimeBy(500)
        coVerify(exactly = 1) { mockUpdateUserProfileUseCase.execute(ProfileAction.Name("Satoshi")) }
    }

    @Test
    fun `setStatusMessage is debounced and triggers usecase`() = runTest {
        val userFlow = MutableStateFlow<User?>(null)
        every { mockGetSelfUserUseCase.execute() } returns userFlow
        val viewModel = createViewModel()

        viewModel.setStatusMessage("Hello")
        viewModel.setStatusMessage("Hello World")
        
        advanceTimeBy(900)
        coVerify(exactly = 1) { mockUpdateUserProfileUseCase.execute(ProfileAction.StatusMessage("Hello World")) }
    }

    @Test
    fun `setStatus triggers usecase immediately`() = runTest {
        val userFlow = MutableStateFlow<User?>(null)
        every { mockGetSelfUserUseCase.execute() } returns userFlow
        val viewModel = createViewModel()

        viewModel.setStatus(UserStatus.Away)
        
        advanceTimeBy(100) // Small wait for coroutine launch
        coVerify(exactly = 1) { mockUpdateUserProfileUseCase.execute(ProfileAction.Status(UserStatus.Away)) }
    }

    @Test
    fun `saveAvatar success path updates cropState and reloads avatar`() = runTest {
        val userFlow = MutableStateFlow<User?>(null)
        every { mockGetSelfUserUseCase.execute() } returns userFlow
        val mockFile = mockk<File>(relaxed = true)
        every { mockFile.exists() } returns false
        every { mockGetSelfAvatarUseCase.execute() } returns mockFile
        coEvery { mockSaveAvatarUseCase.execute(any()) } returns true
        
        val viewModel = createViewModel()

        viewModel.cropState.test {
            assertEquals(AvatarCropUiState.Idle, awaitItem())

            viewModel.saveAvatar(byteArrayOf(1, 2, 3))

            assertEquals(AvatarCropUiState.Success, awaitItem())
        }
    }

    @Test
    fun `saveAvatar failure path updates cropState`() = runTest {
        val userFlow = MutableStateFlow<User?>(null)
        every { mockGetSelfUserUseCase.execute() } returns userFlow
        val mockFile = mockk<File>(relaxed = true)
        every { mockFile.exists() } returns false
        every { mockGetSelfAvatarUseCase.execute() } returns mockFile
        coEvery { mockSaveAvatarUseCase.execute(any()) } returns false
        
        val viewModel = createViewModel()

        viewModel.cropState.test {
            assertEquals(AvatarCropUiState.Idle, awaitItem())

            viewModel.saveAvatar(byteArrayOf(1, 2, 3))

            assertEquals(AvatarCropUiState.Failure, awaitItem())
        }
    }

    @Test
    fun `deleteProfileAndData calls usecase`() = runTest {
        val userFlow = MutableStateFlow<User?>(null)
        every { mockGetSelfUserUseCase.execute() } returns userFlow
        val viewModel = createViewModel()

        viewModel.deleteProfileAndData()
        
        advanceTimeBy(100) // Small wait for coroutine launch
        coVerify(exactly = 1) { mockDeleteProfileUseCase.execute() }
    }

    @Test
    fun `broadcastAvatar calls usecase`() = runTest {
        val userFlow = MutableStateFlow<User?>(null)
        every { mockGetSelfUserUseCase.execute() } returns userFlow
        val viewModel = createViewModel()

        viewModel.broadcastAvatar()
        
        advanceTimeBy(100)
        coVerify(exactly = 1) { mockBroadcastAvatarUseCase.execute() }
    }
}
