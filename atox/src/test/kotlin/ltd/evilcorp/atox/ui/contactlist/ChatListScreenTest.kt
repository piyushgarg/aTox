package ltd.evilcorp.atox.ui.contactlist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.isRoot
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.runtime.CompositionLocalProvider
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider
import ltd.evilcorp.domain.core.network.IFileStorageProvider

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class ChatListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeFileStorageProvider = object : IFileStorageProvider {
        override fun exists(uriString: String): Boolean = false
        override fun lastModified(uriString: String): Long = 0L
        override fun size(uriString: String): Long = 0L
        override fun getAbsolutePath(uriString: String): String? = null
    }

    @Test
    fun testChatListScreen_showsAppNameAndSearch() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides fakeFileStorageProvider) {
                ChatsRouteScreen(
                    connectionStatus = ConnectionStatus.None,
                    contacts = emptyList(),
                    friendRequests = emptyList(),
                    groupInvite = null,
                    groupInviteFriendName = "",
                    dateFormatPreference = DateFormatPreference.System,
                    timeFormatPreference = TimeFormatPreference.System,
                    onContactClick = {},
                    onDeleteContact = {},
                    onAcceptFriendRequest = {},
                    onRejectFriendRequest = {},
                    onAcceptGroupInvite = {},
                    onRejectGroupInvite = {},
                    onAddContactClick = {},
                    onContactInteraction = {},
                    onSearchClick = {},
                    groupsState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) },
                    connectionStatusesState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap()) },
                    onGroupClick = {},
                    onLeaveGroup = {}
                )
            }
        }

        composeTestRule.onNode(androidx.compose.ui.test.hasText("aTox", substring = true, ignoreCase = true)).assertExists()
        composeTestRule.onNodeWithContentDescription("Search").assertExists()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun chatListScreen_layout_matchesSnapshot() {
        val sampleContacts = listOf(
            ltd.evilcorp.domain.features.contacts.model.Contact(
                publicKey = "1",
                name = "Alice",
                statusMessage = "0000",
                status = ltd.evilcorp.domain.features.contacts.model.UserStatus.None,
                hasUnreadMessages = true,
                draftMessage = "Draft: Hello..."
            ),
            ltd.evilcorp.domain.features.contacts.model.Contact(
                publicKey = "2",
                name = "Bob",
                statusMessage = "1111",
                status = ltd.evilcorp.domain.features.contacts.model.UserStatus.Away
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides fakeFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ChatsRouteScreen(
                        connectionStatus = ConnectionStatus.TCP,
                        contacts = sampleContacts,
                        friendRequests = emptyList(),
                        groupInvite = null,
                        groupInviteFriendName = "",
                        dateFormatPreference = DateFormatPreference.System,
                        timeFormatPreference = TimeFormatPreference.System,
                        onContactClick = {},
                        onDeleteContact = {},
                        onAcceptFriendRequest = {},
                        onRejectFriendRequest = {},
                        onAcceptGroupInvite = {},
                        onRejectGroupInvite = {},
                        onAddContactClick = {},
                        onContactInteraction = {},
                        onSearchClick = {},
                        groupsState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) },
                        connectionStatusesState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap()) },
                        onGroupClick = {},
                        onLeaveGroup = {}
                    )
                }
            }
        }
        
        composeTestRule.onNode(isRoot()).captureRoboImage(
            filePath = "src/test/snapshots/images/chat_list_screen_unread_and_draft.png"
        )
    }
}
