package ltd.evilcorp.atox.ui.contactlist

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.atox.MainDispatcherRule
import ltd.evilcorp.atox.SharedContent
import ltd.evilcorp.atox.infrastructure.sharing.SharedContentRegistry
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.chat.usecase.SendChatMessageUseCase
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.contacts.usecase.DeleteContactUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetContactUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetContactsUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetFriendPublicKeyUseCase
import ltd.evilcorp.domain.features.contacts.usecase.GetFriendRequestsUseCase
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.usecase.AcceptGroupInviteUseCase
import ltd.evilcorp.domain.features.group.usecase.DeclineGroupInviteUseCase
import ltd.evilcorp.domain.features.group.usecase.GetGroupInviteUseCase
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.usecase.GetToxRunningStateUseCase
import ltd.evilcorp.domain.features.settings.usecase.GetUserSettingsUseCase
import ltd.evilcorp.domain.features.transfer.usecase.ManageFileTransferUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContactListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockGetContactsUseCase = mockk<GetContactsUseCase>()
    private val mockGetGroupInviteUseCase = mockk<GetGroupInviteUseCase>()
    private val mockGetFriendRequestsUseCase = mockk<GetFriendRequestsUseCase>()
    private val mockGetSelfUserUseCase = mockk<GetSelfUserUseCase>()
    private val mockGetToxRunningStateUseCase = mockk<GetToxRunningStateUseCase>()
    private val mockGetUserSettingsUseCase = mockk<GetUserSettingsUseCase>()
    private val mockGetFriendPublicKeyUseCase = mockk<GetFriendPublicKeyUseCase>()
    private val mockGetContactUseCase = mockk<GetContactUseCase>()
    private val mockDeleteContactUseCase = mockk<DeleteContactUseCase>(relaxed = true)
    private val mockAcceptGroupInviteUseCase = mockk<AcceptGroupInviteUseCase>(relaxed = true)
    private val mockDeclineGroupInviteUseCase = mockk<DeclineGroupInviteUseCase>(relaxed = true)
    private val mockSendChatMessageUseCase = mockk<SendChatMessageUseCase>(relaxed = true)
    private val mockManageFileTransferUseCase = mockk<ManageFileTransferUseCase>(relaxed = true)
    private val mockSharedContentRegistry = mockk<SharedContentRegistry>(relaxed = true)

    private fun createViewModel(): ContactListViewModel {
        every { mockGetContactsUseCase.execute() } returns MutableStateFlow(emptyList())
        every { mockGetGroupInviteUseCase.execute() } returns MutableStateFlow<GroupInvite?>(null)
        every { mockGetFriendRequestsUseCase.execute() } returns MutableStateFlow(emptyList<FriendRequest>())
        every { mockGetSelfUserUseCase.publicKey } returns PublicKey("ABCDEF")
        every { mockGetSelfUserUseCase.execute() } returns MutableStateFlow<User?>(null)
        every { mockSharedContentRegistry.sharedContent } returns MutableStateFlow<SharedContent?>(null)
        val defaultSettings = UserSettings()
        every { mockGetUserSettingsUseCase.settings } returns MutableStateFlow(defaultSettings)

        return ContactListViewModel(
            mockGetContactsUseCase,
            mockGetGroupInviteUseCase,
            mockGetFriendRequestsUseCase,
            mockGetSelfUserUseCase,
            mockGetToxRunningStateUseCase,
            mockGetUserSettingsUseCase,
            mockGetFriendPublicKeyUseCase,
            mockGetContactUseCase,
            mockDeleteContactUseCase,
            mockAcceptGroupInviteUseCase,
            mockDeclineGroupInviteUseCase,
            mockSendChatMessageUseCase,
            mockManageFileTransferUseCase,
            mockSharedContentRegistry
        )
    }

    @Test
    fun `clearSharedContent calls registry clear`() = runTest {
        val viewModel = createViewModel()
        viewModel.clearSharedContent()
        verify { mockSharedContentRegistry.clear() }
    }

    @Test
    fun `setSearchQuery updates query`() = runTest {
        val viewModel = createViewModel()
        viewModel.setSearchQuery("test query")
        assertEquals("test query", viewModel.searchQuery.value)
    }

    @Test
    fun `deleteContact calls use case`() = runTest {
        val viewModel = createViewModel()
        val pk = PublicKey("AABBCC")
        viewModel.deleteContact(pk)
        io.mockk.coVerify { mockDeleteContactUseCase.execute(pk) }
    }

    @Test
    fun `prepareOpenChat sets selectedChatSnapshot`() = runTest {
        val viewModel = createViewModel()
        val contact = Contact(publicKey = "AABBCC", name = "Friend")
        viewModel.prepareOpenChat(contact)
        assertEquals(contact, viewModel.selectedChatSnapshot.value)
    }
}
