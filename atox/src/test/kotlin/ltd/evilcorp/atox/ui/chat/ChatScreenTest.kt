@file:Suppress("MaxLineLength")
package ltd.evilcorp.atox.ui.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.call.service.IVoiceRecorder
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ltd.evilcorp.domain.core.network.IFileStorageProvider
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider
import androidx.compose.runtime.CompositionLocalProvider

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysChatContent() {
        val mockFileStorageProvider = mockk<IFileStorageProvider>(relaxed = true)
        val mockSystemSoundPlayer = mockk<SystemSoundPlayer>(relaxed = true)
        val mockVoiceRecorder = mockk<IVoiceRecorder>(relaxed = true)

        val contact = Contact(publicKey = "PK1", name = "Alice")
        val messages = listOf(
            Message(publicKey = "PK1", message = "Hello Alice", sender = ltd.evilcorp.domain.features.chat.model.Sender.Sent, type = ltd.evilcorp.domain.features.chat.model.MessageType.Normal, correlationId = 1, id = 1),
            Message(publicKey = "PK1", message = "Hi there!", sender = ltd.evilcorp.domain.features.chat.model.Sender.Received, type = ltd.evilcorp.domain.features.chat.model.MessageType.Normal, correlationId = 2, id = 2)
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalFileStorageProvider provides mockFileStorageProvider
            ) {
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
        
        composeTestRule.onNodeWithText("Hello Alice").assertExists()
        composeTestRule.onNodeWithText("Hi there!").assertExists()
    }

    @Test
    fun triggersOnBackCallback() {
        var backClicked = false
        val mockFileStorageProvider = mockk<IFileStorageProvider>(relaxed = true)
        val mockSystemSoundPlayer = mockk<SystemSoundPlayer>(relaxed = true)
        val mockVoiceRecorder = mockk<IVoiceRecorder>(relaxed = true)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalFileStorageProvider provides mockFileStorageProvider
            ) {
                androidx.compose.material3.MaterialTheme {
                    ChatScreen(
                        uiState = ChatUiState(
                            contact = Contact(publicKey = "PK1", name = "Alice"),
                            messages = emptyList(),
                            fileTransfers = emptyList(),
                            replyingToMessage = null,
                            uiConfig = null
                        ),
                        onBack = { backClicked = true },
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
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(backClicked)
    }

    @Test
    fun chatScreen_ux_extremeLongInput_and_rapidSendClicks() {
        var sendCount = 0
        var sentMessage = ""
        val mockFileStorageProvider = mockk<IFileStorageProvider>(relaxed = true)
        val mockSystemSoundPlayer = mockk<SystemSoundPlayer>(relaxed = true)
        val mockVoiceRecorder = mockk<IVoiceRecorder>(relaxed = true)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalFileStorageProvider provides mockFileStorageProvider
            ) {
                androidx.compose.material3.MaterialTheme {
                    ChatScreen(
                        uiState = ChatUiState(
                            contact = Contact(publicKey = "PK1", name = "Alice"),
                            messages = emptyList(),
                            fileTransfers = emptyList(),
                            replyingToMessage = null,
                            uiConfig = null
                        ),
                        onBack = {},
                        onSendMessage = {
                            sendCount++
                            sentMessage = it
                        },
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

        // Send button is hidden initially
        composeTestRule.onNodeWithContentDescription("Send", ignoreCase = true).assertDoesNotExist()

        // 1. Extreme long input test
        val longString = "A".repeat(5000)
        composeTestRule.onNode(hasSetTextAction()).performTextInput(longString)
        
        // 2. Rapid Tapping (Stress test) - attempt to click send 10 times quickly
        val sendNode = composeTestRule.onNodeWithContentDescription("Send", ignoreCase = true)
        sendNode.assertExists()
        repeat(10) {
            try {
                sendNode.performClick()
            } catch (e: AssertionError) {
                // Node might be removed from the tree because text is cleared instantly
            }
        }

        // Even with 10 clicks, it should only trigger once because the text is cleared instantly
        assert(sendCount == 1) { "Send triggered $sendCount times instead of 1!" }
        assert(sentMessage == longString)

        // 3. Clear text check
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Typing...")
        composeTestRule.onNode(hasSetTextAction()).performTextClearance()
        composeTestRule.onNodeWithContentDescription("Send", ignoreCase = true).assertDoesNotExist()
    }
}
