package ltd.evilcorp.atox.ui.groupchat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider
import ltd.evilcorp.domain.core.network.IFileStorageProvider
import ltd.evilcorp.domain.features.call.service.IVoiceRecorder
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import ltd.evilcorp.atox.MainDispatcherRule

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class GroupChatUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockFileStorageProvider = mockk<IFileStorageProvider>(relaxed = true)
    private val mockSystemSoundPlayer = mockk<SystemSoundPlayer>(relaxed = true)
    private val mockVoiceRecorder = mockk<IVoiceRecorder>(relaxed = true)

    @Test
    fun testCreateGroupScreen_interactions() {
        var createGroupCalled = false
        var passedName = ""
        var passedPrivacy = GroupPrivacyState.Public
        var passedPassword: String? = null

        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                CreateGroupScreen(
                    onBack = {},
                    isCreatingState = MutableStateFlow(false),
                    onCreateGroup = { name, privacy, pass ->
                        createGroupCalled = true
                        passedName = name
                        passedPrivacy = privacy
                        passedPassword = pass
                        true
                    }
                )
            }
        }

        // Enter group name
        composeTestRule.onNodeWithText("Group Name").performTextInput("Super Developers")

        // Switch privacy state to Private
        composeTestRule.onNodeWithText("Private").performClick()

        // Wait for AnimatedVisibility transition to complete
        composeTestRule.waitForIdle()

        // Type password in password field
        // AtoxPasswordField usually has a specific label, let's find the text field for password
        // Since there are two text fields now (Name and Password), let's find the one matching password
        composeTestRule.onNodeWithText("Group Password").performTextInput("secure123")

        // Tap Create button
        composeTestRule.onNode(hasText("Create") and hasClickAction()).performScrollTo().performClick()

        // Wait for coroutine inside onClick scope to execute
        composeTestRule.waitForIdle()

        assertTrue(createGroupCalled)
        assertEquals("Super Developers", passedName)
        assertEquals(GroupPrivacyState.Private, passedPrivacy)
        assertEquals("secure123", passedPassword)
    }

    @Test
    fun testJoinGroupScreen_interactions() {
        var joinGroupCalled = false
        var passedChatId = ""
        var passedPassword: String? = null

        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                JoinGroupScreen(
                    onBack = {},
                    isJoiningState = MutableStateFlow(false),
                    onValidateChatId = { null }, // Valid always
                    onJoinGroup = { chatId, pass ->
                        joinGroupCalled = true
                        passedChatId = chatId
                        passedPassword = pass
                        true
                    }
                )
            }
        }

        val testChatId = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"

        // Type valid Chat ID
        composeTestRule.onNodeWithText("Group Chat ID").performTextInput(testChatId)

        // Type password
        composeTestRule.onNodeWithText("Group Password").performTextInput("joinpass")

        // Click Join Group button
        composeTestRule.onNode(hasText("Join Group") and hasClickAction()).performClick()

        // Wait for coroutine inside onClick scope to execute
        composeTestRule.waitForIdle()

        assertTrue(joinGroupCalled)
        assertEquals(testChatId, passedChatId)
        assertEquals("joinpass", passedPassword)
    }

    @Test
    fun testGroupChatScreen_rendersMessagesAndSends() {
        var sentMessageText = ""

        val group = Group(
            chatId = "GROUP_ID_123",
            name = "Atox Devs",
            topic = "Compose unit tests discussion"
        )
        val message = GroupMessage(
            groupChatId = "GROUP_ID_123",
            peerId = 1,
            senderName = "Bob",
            message = "Welcome to the group chat",
            sender = Sender.Received,
            type = MessageType.Normal,
            correlationId = 0,
            timestamp = System.currentTimeMillis()
        )
        val peer1 = GroupPeer(groupChatId = "GROUP_ID_123", peerId = 1, name = "Bob", publicKey = "BOB_PK")
        val peer2 = GroupPeer(groupChatId = "GROUP_ID_123", peerId = 2, name = "Me", publicKey = "OUR_PK", isOurselves = true)

        val groupState = mutableStateOf<Group?>(group)
        val messagesState = mutableStateOf<List<GroupMessage>?>(listOf(message))
        val peersState = mutableStateOf<List<GroupPeer>?>(listOf(peer1, peer2))
        val contactsState = mutableStateOf<List<Contact>>(emptyList())
        val connectionStatusState = mutableStateOf(GroupConnectionStatus.Connected)
        val fileTransfersState = mutableStateOf<List<FileTransfer>>(emptyList())
        val selfAvatarUriState = mutableStateOf("")

        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    GroupChatScreen(
                        groupState = groupState,
                        messagesState = messagesState,
                        peersState = peersState,
                        contactsState = contactsState,
                        connectionStatusState = connectionStatusState,
                        fileTransfersState = fileTransfersState,
                        selfAvatarUriState = selfAvatarUriState,
                        uiConfig = ChatUiConfig(
                            hapticEnabled = false,
                            dateFormatPreference = ltd.evilcorp.domain.features.settings.model.DateFormatPreference.System,
                            timeFormatPreference = ltd.evilcorp.domain.features.settings.model.TimeFormatPreference.System,
                            sentMessageSoundUri = "",
                            sentMessageSoundVolume = 0,
                            enableReplies = true
                        ),
                        onBack = {},
                        onSendMessage = { sentMessageText = it },
                        onSendFile = {},
                        onSendVoice = {},
                        onAcceptFt = {},
                        onRejectFt = {},
                        onCancelFt = {},
                        onSaveAsClick = { _, _ -> },
                        onOpenFile = {},
                        onLeaveGroup = {},
                        onCopyInvite = {},
                        onInviteFriend = {},
                        systemSoundPlayer = mockSystemSoundPlayer,
                        voiceRecorder = mockVoiceRecorder
                    )
                }
            }
        }

        // Verify group name in app bar
        composeTestRule.onNodeWithText("Atox Devs").assertIsDisplayed()

        // Verify message list shows message and sender name
        composeTestRule.onNodeWithText("Welcome to the group chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()

        // Type group message and click Send
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Hey Bob!")
        composeTestRule.onNodeWithContentDescription("Send", ignoreCase = true).performClick()

        assertEquals("Hey Bob!", sentMessageText)
    }
}
