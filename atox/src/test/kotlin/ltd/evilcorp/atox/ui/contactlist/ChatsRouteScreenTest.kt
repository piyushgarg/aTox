package ltd.evilcorp.atox.ui.contactlist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class ChatsRouteScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysContactList() {
        val mockFileStorageProvider = io.mockk.mockk<ltd.evilcorp.domain.core.network.IFileStorageProvider>(relaxed = true)

        composeTestRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                ltd.evilcorp.atox.ui.common.LocalFileStorageProvider provides mockFileStorageProvider
            ) {
                androidx.compose.material3.MaterialTheme {
                    ChatsRouteScreen(
                        connectionStatus = ConnectionStatus.TCP,
                        contacts = listOf(
                            Contact(publicKey = "PK1", name = "Alice"),
                            Contact(publicKey = "PK2", name = "Bob")
                        ),
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
        
        composeTestRule.onNodeWithText("Alice").assertExists()
        composeTestRule.onNodeWithText("Bob").assertExists()
    }

    @Test
    fun clickingContactTriggersCallback() {
        var clickedContact: Contact? = null
        val mockFileStorageProvider = io.mockk.mockk<ltd.evilcorp.domain.core.network.IFileStorageProvider>(relaxed = true)
        
        composeTestRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                ltd.evilcorp.atox.ui.common.LocalFileStorageProvider provides mockFileStorageProvider
            ) {
                androidx.compose.material3.MaterialTheme {
                    ChatsRouteScreen(
                        connectionStatus = ConnectionStatus.TCP,
                        contacts = listOf(
                            Contact(publicKey = "PK1", name = "Alice"),
                            Contact(publicKey = "PK2", name = "Bob")
                        ),
                        friendRequests = emptyList(),
                        groupInvite = null,
                        groupInviteFriendName = "",
                        dateFormatPreference = DateFormatPreference.System,
                        timeFormatPreference = TimeFormatPreference.System,
                        onContactClick = { clickedContact = it },
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
        
        composeTestRule.onNodeWithText("Alice").performClick()
        assert(clickedContact?.publicKey == "PK1")
    }
}
