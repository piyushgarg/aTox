package ltd.evilcorp.atox.ui.contactlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.ui.theme.AToxTheme
import ltd.evilcorp.atox.ui.contactlist.components.*
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.DateFormatPreference
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.model.TimeFormatPreference
import ltd.evilcorp.core.model.UserStatus

@Preview(name = "Chat list empty", showBackground = true)
@Composable
private fun EmptyChatListPreview() {
    AToxTheme {
        EmptyChatList(onAddContactClick = {})
    }
}

@Preview(name = "Chat list mixed", showBackground = true)
@Composable
private fun ChatListTabPreview() {
    AToxTheme {
        ChatListTab(
            contacts = previewContacts,
            friendRequests = previewFriendRequests,
            groupInvite = null,
            groupInviteFriendName = "",
            listState = rememberLazyListState(),
            searchQuery = "",
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
        )
    }
}

@Preview(name = "Chat rows", showBackground = true)
@Composable
private fun ContactRowsPreview() {
    AToxTheme {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            previewContacts.forEach { contact ->
                ContactItemCard(
                    contact = contact,
                    dateFormatPreference = DateFormatPreference.System,
                    timeFormatPreference = TimeFormatPreference.System,
                    onClick = {},
                    onDelete = {},
                )
            }
        }
    }
}

@Preview(name = "Friend request", showBackground = true)
@Composable
private fun FriendRequestPreview() {
    AToxTheme {
        FriendRequestItemCard(
            request = previewFriendRequests.first(),
            onAccept = {},
            onReject = {},
        )
    }
}

private val previewContacts = listOf(
    Contact(
        publicKey = "A1B2C3D4E5F6",
        name = "Sergey",
        statusMessage = "Online",
        lastMessage = System.currentTimeMillis(),
        connectionStatus = ConnectionStatus.TCP,
        status = UserStatus.None,
        hasUnreadMessages = true,
    ),
    Contact(
        publicKey = "B1C2D3E4F5A6",
        name = "Long display name that must stay readable",
        statusMessage = "Typing preview",
        lastMessage = System.currentTimeMillis() - 60_000,
        connectionStatus = ConnectionStatus.UDP,
        typing = true,
    ),
    Contact(
        publicKey = "C1D2E3F4A5B6",
        name = "",
        statusMessage = "Offline but with a draft",
        lastMessage = System.currentTimeMillis() - 86_400_000,
        draftMessage = "Need to send backup details later",
    ),
)

private val previewFriendRequests = listOf(
    FriendRequest(
        publicKey = "F00DBABE1234567890",
        message = "Add me to continue the secure chat.",
    )
)
