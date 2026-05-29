package ltd.evilcorp.atox.ui.contactlist.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.atox.ui.navigation.LocalTabPadding

private val ContentPaddingTop = 4.dp
private val ContentPaddingBottomDefault = 16.dp
private val InvitePaddingHorizontal = 16.dp
private val InvitePaddingVertical = 8.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListTab(
    contacts: List<Contact>,
    friendRequests: List<FriendRequest>,
    groupInvite: GroupInvite?,
    groupInviteFriendName: String,
    listState: LazyListState,
    dateFormatPreference: DateFormatPreference,
    timeFormatPreference: TimeFormatPreference,
    onContactClick: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onAcceptFriendRequest: (FriendRequest) -> Unit,
    onRejectFriendRequest: (FriendRequest) -> Unit,
    onAcceptGroupInvite: () -> Unit,
    onRejectGroupInvite: () -> Unit,
    onAddContactClick: () -> Unit,
    onContactInteraction: () -> Unit,
) {
    val visibleContacts = contacts

    if (friendRequests.isEmpty() && visibleContacts.isEmpty() && groupInvite == null) {
        EmptyChatList(onAddContactClick = onAddContactClick)
        return
    }

    val bottomPadding = LocalTabPadding.current.calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            top = ContentPaddingTop,
            bottom = ContentPaddingBottomDefault + bottomPadding
        )
    ) {
        if (groupInvite != null) {
            item(key = "group_invite", contentType = "group_invite") {
                GroupInviteItemCard(
                    invite = groupInvite,
                    friendName = groupInviteFriendName,
                    onAccept = onAcceptGroupInvite,
                    onReject = onRejectGroupInvite,
                    modifier = Modifier.padding(
                        horizontal = InvitePaddingHorizontal,
                        vertical = InvitePaddingVertical
                    )
                )
            }
        }

        if (friendRequests.isNotEmpty()) {
            items(
                items = friendRequests,
                key = { request -> "request:${request.publicKey}" },
                contentType = { "friend_request" },
            ) { request ->
                FriendRequestItemCard(
                    request = request,
                    onAccept = { onAcceptFriendRequest(request) },
                    onReject = { onRejectFriendRequest(request) },
                    modifier = Modifier
                )
            }
        }

        items(
            items = visibleContacts,
            key = { contact -> contact.publicKey },
            contentType = { "contact" },
        ) { contact ->
            ContactItemCard(
                contact = contact,
                dateFormatPreference = dateFormatPreference,
                timeFormatPreference = timeFormatPreference,
                onClick = {
                    onContactInteraction()
                    onContactClick(contact)
                },
                onDelete = { onDeleteContact(contact) },
                modifier = Modifier
            )
        }
    }
}
