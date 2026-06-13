package ltd.evilcorp.atox.ui.friendrequest

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.atox.MainDispatcherRule
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.contacts.usecase.AcceptFriendRequestUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetFriendRequestsUseCase
import ltd.evilcorp.domain.features.contacts.usecase.RejectFriendRequestUseCase
import org.junit.Rule
import org.junit.Test

class FriendRequestsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockGetFriendRequestsUseCase = mockk<GetFriendRequestsUseCase>()
    private val mockAcceptFriendRequestUseCase = mockk<AcceptFriendRequestUseCase>(relaxed = true)
    private val mockRejectFriendRequestUseCase = mockk<RejectFriendRequestUseCase>(relaxed = true)

    private fun createViewModel(): FriendRequestsViewModel {
        every { mockGetFriendRequestsUseCase.execute() } returns MutableStateFlow<List<FriendRequest>>(emptyList())
        return FriendRequestsViewModel(
            mockGetFriendRequestsUseCase,
            mockAcceptFriendRequestUseCase,
            mockRejectFriendRequestUseCase
        )
    }

    @Test
    fun `acceptFriendRequest calls use case`() = runTest {
        val viewModel = createViewModel()
        val request = FriendRequest("ABCDEF", "message")
        
        viewModel.acceptFriendRequest(request)
        
        verify { mockAcceptFriendRequestUseCase.execute(request) }
    }

    @Test
    fun `rejectFriendRequest calls use case`() = runTest {
        val viewModel = createViewModel()
        val request = FriendRequest("ABCDEF", "message")
        
        viewModel.rejectFriendRequest(request)
        
        verify { mockRejectFriendRequestUseCase.execute(request) }
    }
}
