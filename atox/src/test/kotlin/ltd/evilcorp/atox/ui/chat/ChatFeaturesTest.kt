package ltd.evilcorp.atox.ui.chat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider
import ltd.evilcorp.domain.core.network.IFileStorageProvider
import ltd.evilcorp.domain.features.call.service.IVoiceRecorder
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
@Suppress("LargeClass")
class ChatFeaturesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockFileStorageProvider = mockk<IFileStorageProvider>(relaxed = true)
    private val mockSystemSoundPlayer = mockk<SystemSoundPlayer>(relaxed = true)
    private val mockVoiceRecorder = mockk<IVoiceRecorder>(relaxed = true)

    @Test
    fun testChatScreenRendering_rendersContactDetailsAndMessages() {
        val contact = Contact(
            publicKey = "CONTACT_PK",
            name = "Alice Smith",
            connectionStatus = ConnectionStatus.TCP
        )
        val messages = listOf(
            Message(
                publicKey = "CONTACT_PK",
                message = "Hello from Alice",
                sender = Sender.Received,
                type = MessageType.Normal,
                correlationId = 0,
                id = 1
            ),
            Message(
                publicKey = "CONTACT_PK",
                message = "Hi Alice",
                sender = Sender.Sent,
                type = MessageType.Normal,
                correlationId = 0,
                id = 2
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ChatScreen(
                        uiState = ChatUiState(
                            contact = contact,
                            messages = messages,
                            fileTransfers = emptyList(),
                            replyingToMessage = null,
                            uiConfig = null
                        ),
                        onBack = {},
                        onSendMessage = {},
                        onTypingChanged = {},
                        onSendFile = {},
                        onCallClick = {},
                        onCallHistoryClick = {},
                        onAcceptFt = {},
                        onRejectFt = {},
                        onCancelFt = {},
                        onSaveFt = { _, _ -> },
                        onOpenFile = {},
                        systemSoundPlayer = mockSystemSoundPlayer,
                        voiceRecorder = mockVoiceRecorder,
                        isTypingFlow = MutableStateFlow(false)
                    )
                }
            }
        }

        // Verify contact name in App Bar
        composeTestRule.onNodeWithText("Alice Smith").assertExists()
        // Verify incoming and outgoing message bubbles
        composeTestRule.onNodeWithText("Hello from Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hi Alice").assertIsDisplayed()
    }

    @Test
    fun testSendMessage_triggersCallback() {
        var sentText = ""
        val contact = Contact(publicKey = "CONTACT_PK", name = "Alice")

        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ChatScreen(
                        uiState = ChatUiState(
                            contact = contact,
                            messages = emptyList(),
                            fileTransfers = emptyList(),
                            replyingToMessage = null,
                            uiConfig = null
                        ),
                        onBack = {},
                        onSendMessage = { sentText = it },
                        onTypingChanged = {},
                        onSendFile = {},
                        onCallClick = {},
                        onCallHistoryClick = {},
                        onAcceptFt = {},
                        onRejectFt = {},
                        onCancelFt = {},
                        onSaveFt = { _, _ -> },
                        onOpenFile = {},
                        systemSoundPlayer = mockSystemSoundPlayer,
                        voiceRecorder = mockVoiceRecorder,
                        isTypingFlow = MutableStateFlow(false)
                    )
                }
            }
        }

        // Enter text into input field
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Testing 123")
        // Press Send button
        composeTestRule.onNodeWithContentDescription("Send", ignoreCase = true).performClick()

        assertEquals("Testing 123", sentText)
    }

    @Test
    fun testFileTransfer_incomingWaiting_showsAcceptDeclineButtons() {
        val contact = Contact(publicKey = "CONTACT_PK", name = "Alice")
        var acceptClickedId = -1
        var rejectClickedId = -1

        val incomingTransfer = FileTransfer(
            publicKey = "CONTACT_PK",
            fileNumber = 0,
            fileKind = 0,
            fileSize = 1024L * 1024L,
            fileName = "document.pdf",
            outgoing = false,
            progress = -1L, // FT_NOT_STARTED
            destination = "",
            id = 42
        )
        val messages = listOf(
            Message(
                publicKey = "CONTACT_PK",
                message = "document.pdf",
                sender = Sender.Received,
                type = MessageType.FileTransfer,
                correlationId = 42,
                id = 1
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ChatScreen(
                        uiState = ChatUiState(
                            contact = contact,
                            messages = messages,
                            fileTransfers = listOf(incomingTransfer),
                            replyingToMessage = null,
                            uiConfig = null
                        ),
                        onBack = {},
                        onSendMessage = {},
                        onTypingChanged = {},
                        onSendFile = {},
                        onCallClick = {},
                        onCallHistoryClick = {},
                        onAcceptFt = { acceptClickedId = it },
                        onRejectFt = { rejectClickedId = it },
                        onCancelFt = {},
                        onSaveFt = { _, _ -> },
                        onOpenFile = {},
                        systemSoundPlayer = mockSystemSoundPlayer,
                        voiceRecorder = mockVoiceRecorder,
                        isTypingFlow = MutableStateFlow(false)
                    )
                }
            }
        }

        // Check file name is shown
        composeTestRule.onNodeWithText("document.pdf").assertIsDisplayed()

        // Click Accept button
        composeTestRule.onNodeWithContentDescription("Accept").performClick()
        assertEquals(42, acceptClickedId)

        // Click Decline button
        composeTestRule.onNodeWithContentDescription("Decline").performClick()
        assertEquals(42, rejectClickedId)
    }

    @Test
    fun testFileTransfer_imageThumbnailRendering() {
        val contact = Contact(publicKey = "CONTACT_PK", name = "Alice")
        val imageTransfer = FileTransfer(
            publicKey = "CONTACT_PK",
            fileNumber = 1,
            fileKind = 0,
            fileSize = 50000L,
            fileName = "photo.jpg",
            outgoing = false,
            progress = 50000L, // Completed
            destination = "file:///sdcard/photo.jpg",
            id = 88
        )
        val messages = listOf(
            Message(
                publicKey = "CONTACT_PK",
                message = "photo.jpg",
                sender = Sender.Received,
                type = MessageType.FileTransfer,
                correlationId = 88,
                id = 1
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ChatScreen(
                        uiState = ChatUiState(
                            contact = contact,
                            messages = messages,
                            fileTransfers = listOf(imageTransfer),
                            replyingToMessage = null,
                            uiConfig = null
                        ),
                        onBack = {},
                        onSendMessage = {},
                        onTypingChanged = {},
                        onSendFile = {},
                        onCallClick = {},
                        onCallHistoryClick = {},
                        onAcceptFt = {},
                        onRejectFt = {},
                        onCancelFt = {},
                        onSaveFt = { _, _ -> },
                        onOpenFile = {},
                        systemSoundPlayer = mockSystemSoundPlayer,
                        voiceRecorder = mockVoiceRecorder,
                        isTypingFlow = MutableStateFlow(false)
                    )
                }
            }
        }

        // Verify the completion card is rendered
        composeTestRule.onNodeWithText("photo.jpg").assertIsDisplayed()
    }

    @Test
    fun testVoiceMessage_showsDownloadOrPlayButton() {
        val contact = Contact(publicKey = "CONTACT_PK", name = "Alice")
        val voiceTransfer = FileTransfer(
            publicKey = "CONTACT_PK",
            fileNumber = 2,
            fileKind = 0,
            fileSize = 10000L,
            fileName = "voice_message_123.amr",
            outgoing = false,
            progress = -1L, // FT_NOT_STARTED
            destination = "",
            id = 99
        )
        val messages = listOf(
            Message(
                publicKey = "CONTACT_PK",
                message = "voice_message_123.amr",
                sender = Sender.Received,
                type = MessageType.FileTransfer,
                correlationId = 99,
                id = 1
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ChatScreen(
                        uiState = ChatUiState(
                            contact = contact,
                            messages = messages,
                            fileTransfers = listOf(voiceTransfer),
                            replyingToMessage = null,
                            uiConfig = null
                        ),
                        onBack = {},
                        onSendMessage = {},
                        onTypingChanged = {},
                        onSendFile = {},
                        onCallClick = {},
                        onCallHistoryClick = {},
                        onAcceptFt = {},
                        onRejectFt = {},
                        onCancelFt = {},
                        onSaveFt = { _, _ -> },
                        onOpenFile = {},
                        systemSoundPlayer = mockSystemSoundPlayer,
                        voiceRecorder = mockVoiceRecorder,
                        isTypingFlow = MutableStateFlow(false)
                    )
                }
            }
        }

        // Should display voice duration timer or "Voice" text and Download audio button
        composeTestRule.onNodeWithContentDescription("Download audio").assertIsDisplayed()
    }

    @Test
    fun testTypingIndicatorAndActionMessageUI() {
        val contact = Contact(publicKey = "CONTACT_PK", name = "Alice")
        val messages = listOf(
            Message(
                publicKey = "CONTACT_PK",
                message = "Call Missed",
                sender = Sender.Received,
                type = MessageType.Action, // Action indicates Call history/event
                correlationId = 0,
                id = 1
            )
        )
        val isTypingFlow = MutableStateFlow(true)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ChatScreen(
                        uiState = ChatUiState(
                            contact = contact,
                            messages = messages,
                            fileTransfers = emptyList(),
                            replyingToMessage = null,
                            uiConfig = null
                        ),
                        onBack = {},
                        onSendMessage = {},
                        onTypingChanged = {},
                        onSendFile = {},
                        onCallClick = {},
                        onCallHistoryClick = {},
                        onAcceptFt = {},
                        onRejectFt = {},
                        onCancelFt = {},
                        onSaveFt = { _, _ -> },
                        onOpenFile = {},
                        systemSoundPlayer = mockSystemSoundPlayer,
                        voiceRecorder = mockVoiceRecorder,
                        isTypingFlow = isTypingFlow
                    )
                }
            }
        }

        // Verify call history action message bubble
        composeTestRule.onNodeWithText("Call Missed").assertIsDisplayed()

        // Typing bubble should exist
        composeTestRule.onNodeWithContentDescription("Typing...", ignoreCase = true).assertExists()
    }

    @Test
    fun testMessageReplyBanner_rendersAndCancels() {
        val contact = Contact(publicKey = "CONTACT_PK", name = "Alice")
        val messageToReply = Message(
            publicKey = "CONTACT_PK",
            message = "Target message to reply",
            sender = Sender.Received,
            type = MessageType.Normal,
            correlationId = 0,
            id = 55
        )
        var cancelReplyCalled = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ChatScreen(
                        uiState = ChatUiState(
                            contact = contact,
                            messages = listOf(messageToReply),
                            fileTransfers = emptyList(),
                            replyingToMessage = messageToReply,
                            uiConfig = null
                        ),
                        onBack = {},
                        onSendMessage = {},
                        onTypingChanged = {},
                        onSendFile = {},
                        onCallClick = {},
                        onCallHistoryClick = {},
                        onAcceptFt = {},
                        onRejectFt = {},
                        onCancelFt = {},
                        onSaveFt = { _, _ -> },
                        onOpenFile = {},
                        systemSoundPlayer = mockSystemSoundPlayer,
                        voiceRecorder = mockVoiceRecorder,
                        isTypingFlow = MutableStateFlow(false),
                        onCancelReply = { cancelReplyCalled = true }
                    )
                }
            }
        }

        // Replying banner should display the replied message content
        composeTestRule.onAllNodesWithText("Target message to reply").assertCountEquals(2)

        // Cancel reply button should cancel reply
        composeTestRule.onNodeWithContentDescription("Cancel reply").performClick()
        assertTrue(cancelReplyCalled)
    }
}
