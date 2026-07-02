package ltd.evilcorp.atox.ui.chat

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.atox.MainDispatcherRule
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.INotificationHelper
import ltd.evilcorp.domain.features.call.CallState
import ltd.evilcorp.domain.features.call.service.IVoiceRecorder
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import androidx.paging.PagingData
import kotlinx.coroutines.flow.flowOf
import ltd.evilcorp.domain.features.chat.usecase.GetChatMessagesPagedUseCase
import ltd.evilcorp.domain.features.chat.usecase.ClearChatHistoryUseCase
import ltd.evilcorp.domain.features.chat.usecase.DeleteChatMessageUseCase
import ltd.evilcorp.domain.features.chat.usecase.GetChatMessagesUseCase
import ltd.evilcorp.domain.features.chat.usecase.SendChatMessageUseCase
import ltd.evilcorp.domain.features.chat.usecase.SetActiveChatUseCase
import ltd.evilcorp.domain.features.chat.usecase.SetChatDraftUseCase
import ltd.evilcorp.domain.features.chat.usecase.SetTypingStatusUseCase
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.usecase.GetContactUseCase
import ltd.evilcorp.domain.features.group.usecase.DeclineGroupInviteUseCase
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.usecase.GetUserSettingsUseCase
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockChatCallDelegate = mockk<ChatCallDelegate>(relaxed = true)
    private val mockChatFileTransferDelegate = mockk<ChatFileTransferDelegate>(relaxed = true)
    private val mockGetContactUseCase = mockk<GetContactUseCase>()
    private val mockGetChatMessagesUseCase = mockk<GetChatMessagesUseCase>()
    private val mockGetChatMessagesPagedUseCase = mockk<GetChatMessagesPagedUseCase>()
    private val mockGetUserSettingsUseCase = mockk<GetUserSettingsUseCase>()
    private val mockSetActiveChatUseCase = mockk<SetActiveChatUseCase>(relaxed = true)
    private val mockSetChatDraftUseCase = mockk<SetChatDraftUseCase>(relaxed = true)
    private val mockNotificationHelper = mockk<INotificationHelper>(relaxed = true)
    private val mockSendChatMessageUseCase = mockk<SendChatMessageUseCase>(relaxed = true)
    private val mockClearChatHistoryUseCase = mockk<ClearChatHistoryUseCase>(relaxed = true)
    private val mockSetTypingStatusUseCase = mockk<SetTypingStatusUseCase>(relaxed = true)
    private val mockDeleteChatMessageUseCase = mockk<DeleteChatMessageUseCase>(relaxed = true)
    private val mockDeclineGroupInviteUseCase = mockk<DeclineGroupInviteUseCase>(relaxed = true)
    private val mockVoiceRecorder = mockk<IVoiceRecorder>(relaxed = true)

    private fun createViewModel(): ChatViewModel {
        every { mockGetContactUseCase.execute(any()) } returns MutableStateFlow<Contact?>(null)
        every { mockGetChatMessagesUseCase.execute(any()) } returns MutableStateFlow<List<Message>>(emptyList())
        every { mockGetChatMessagesPagedUseCase.execute(any()) } returns flowOf(PagingData.empty())
        val defaultSettings = UserSettings()
        every { mockGetUserSettingsUseCase.settings } returns MutableStateFlow(defaultSettings)
        every { mockChatCallDelegate.ongoingCall } returns MutableStateFlow(CallState.Idle)
        every { mockChatCallDelegate.getCallState(any()) } returns MutableStateFlow(CallAvailability.Unavailable)
        every { mockChatFileTransferDelegate.transfersFor(any()) } returns MutableStateFlow<List<FileTransfer>>(emptyList())

        return ChatViewModel(
            mockChatCallDelegate,
            mockChatFileTransferDelegate,
            mockGetContactUseCase,
            mockGetChatMessagesUseCase,
            mockGetChatMessagesPagedUseCase,
            mockGetUserSettingsUseCase,
            mockSetActiveChatUseCase,
            mockSetChatDraftUseCase,
            mockNotificationHelper,
            mockSendChatMessageUseCase,
            mockClearChatHistoryUseCase,
            mockSetTypingStatusUseCase,
            mockDeleteChatMessageUseCase,
            mockDeclineGroupInviteUseCase,
            mockVoiceRecorder
        )
    }

    @Test
    fun `setActiveChat clears notifications and sets active chat`() = runTest {
        val viewModel = createViewModel()
        val pk = PublicKey("AABBCC")
        
        viewModel.setActiveChat(pk)
        
        coVerify { mockNotificationHelper.dismissNotifications(pk) }
        coVerify { mockSetActiveChatUseCase.execute(pk) }
    }

    @Test
    fun `send message calls usecase with correct reply to id`() = runTest {
        val viewModel = createViewModel()
        val pk = PublicKey("AABBCC")
        viewModel.setActiveChat(pk)
        
        val replyMessage = Message(
            publicKey = pk.string(),
            message = "original message",
            sender = Sender.Sent,
            type = MessageType.Normal,
            correlationId = -1,
            timestamp = 0L,
            id = 10
        )
        viewModel.setReplyingTo(replyMessage)
        
        viewModel.sendMessage("reply", MessageType.Normal)
        
        coVerify { mockSendChatMessageUseCase.execute(pk, "reply", MessageType.Normal, "original message".hashCode()) }
        assertEquals(null, viewModel.replyingToMessage.value)
    }

    @Test
    fun `clearHistory calls usecases`() = runTest {
        val viewModel = createViewModel()
        val pk = PublicKey("AABBCC")
        viewModel.setActiveChat(pk)
        
        viewModel.clearHistory()
        
        coVerify { mockClearChatHistoryUseCase.execute(pk) }
        coVerify { mockChatFileTransferDelegate.clearTransfers(pk) }
    }
}
