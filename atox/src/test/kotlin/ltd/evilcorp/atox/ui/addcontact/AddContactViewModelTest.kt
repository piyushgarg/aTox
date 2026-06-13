package ltd.evilcorp.atox.ui.addcontact

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.delay
import ltd.evilcorp.atox.MainDispatcherRule
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.usecase.AddContactUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetContactsUseCase
import ltd.evilcorp.domain.features.settings.usecase.ManageToxLifecycleUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddContactViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockAddContactUseCase = mockk<AddContactUseCase>(relaxed = true)
    private val mockGetContactsUseCase = mockk<GetContactsUseCase>()
    private val mockGetSelfUserUseCase = mockk<GetSelfUserUseCase>()
    private val mockManageToxLifecycleUseCase = mockk<ManageToxLifecycleUseCase>(relaxed = true)

    private fun createViewModel(): AddContactViewModel {
        every { mockGetSelfUserUseCase.publicKey } returns PublicKey("ABCDEF")
        every { mockGetSelfUserUseCase.execute() } returns MutableStateFlow<User?>(null)
        every { mockGetSelfUserUseCase.toxId } returns ToxID("MY_TOX_ID")
        every { mockGetContactsUseCase.execute() } returns MutableStateFlow<List<Contact>>(emptyList())

        return AddContactViewModel(
            mockAddContactUseCase,
            mockGetContactsUseCase,
            mockGetSelfUserUseCase,
            mockManageToxLifecycleUseCase
        )
    }

    @Test
    fun `addContact with short tox ID sets error`() = runTest {
        val viewModel = createViewModel()
        viewModel.addContact("short", "hello")
        assertEquals(R.string.add_contact_error_invalid, viewModel.errorResId.value)
    }

    @Test
    fun `addContact success emits Success event`() = runTest {
        val viewModel = createViewModel()
        val validToxId = "123456789012345678901234567890123456789012345678901234567890123456789012"
        
        coEvery { mockAddContactUseCase.execute(any(), any()) } returns Unit
        
        viewModel.uiEvents.test {
            viewModel.addContact(validToxId, "hello")
            val event = awaitItem()
            assertTrue(event is AddContactViewModel.AddContactUiEvent.Success)
            cancelAndIgnoreRemainingEvents()
        }
        
        coVerify { mockAddContactUseCase.execute(ToxID(validToxId), "hello") }
    }

    @Test
    fun `addContact failure sets generic error`() = runTest {
        val viewModel = createViewModel()
        val validToxId = "123456789012345678901234567890123456789012345678901234567890123456789012"
        
        coEvery { mockAddContactUseCase.execute(any(), any()) } throws RuntimeException("Error")
        
        viewModel.addContact(validToxId, "hello")
        
        assertEquals(R.string.create_profile_error_failed, viewModel.errorResId.value)
    }

    @Test
    fun `addContact multiple rapid calls only executes once`() = runTest(StandardTestDispatcher()) {
        val viewModel = createViewModel()
        val validToxId = "123456789012345678901234567890123456789012345678901234567890123456789012"
        
        coEvery { mockAddContactUseCase.execute(any(), any()) } coAnswers {
            delay(100) // Simulate long running task
        }
        
        viewModel.addContact(validToxId, "hello")
        viewModel.addContact(validToxId, "hello2")
        viewModel.addContact(validToxId, "hello3")
        
        advanceUntilIdle()
        
        // Should only be called once because isLoading prevents subsequent calls
        coVerify(exactly = 1) { mockAddContactUseCase.execute(any(), any()) }
    }
}
