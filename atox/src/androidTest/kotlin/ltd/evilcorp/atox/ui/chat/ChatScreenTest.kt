package ltd.evilcorp.atox.ui.chat

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.domain.features.call.service.IVoiceRecorder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.runtime.CompositionLocalProvider
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider
import ltd.evilcorp.domain.core.network.IFileStorageProvider

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeContact = Contact(
        publicKey = "fake_pub_key",
        name = "Alice",
        statusMessage = "Hello World",
        status = UserStatus.None,
        connectionStatus = ConnectionStatus.UDP,
        hasUnreadMessages = false,
        lastMessage = 0L,
        draftMessage = "",
        typing = false
    )

    private val fakeUiState = ChatUiState(
        contact = fakeContact,
        messages = emptyList(),
        fileTransfers = emptyList(),
        replyingToMessage = null,
        uiConfig = null
    )

    private val fakeVoiceRecorder = object : IVoiceRecorder {
        override fun startRecording(): Boolean = true
        override suspend fun stopRecording(): String? = null
        override suspend fun cancelRecording() {}
        override fun release() {}
    }

    private val fakeSoundPlayer = SystemSoundPlayer(
        androidx.test.core.app.ApplicationProvider.getApplicationContext()
    )

    private val fakeFileStorageProvider = object : IFileStorageProvider {
        override fun exists(uriString: String): Boolean = false
        override fun lastModified(uriString: String): Long = 0L
        override fun size(uriString: String): Long = 0L
        override fun getAbsolutePath(uriString: String): String? = null
    }

    @Test
    fun testChatScreen_showsContactName() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides fakeFileStorageProvider) {
                ChatScreen(
                    uiState = fakeUiState,
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
                    systemSoundPlayer = fakeSoundPlayer,
                    voiceRecorder = fakeVoiceRecorder
                )
            }
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    }
}
