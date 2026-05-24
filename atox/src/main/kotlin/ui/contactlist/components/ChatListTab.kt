package ltd.evilcorp.atox.ui.contactlist.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.DateFormatPreference
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.model.TimeFormatPreference

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListTab(
    contacts: List<Contact>,
    friendRequests: List<FriendRequest>,
    listState: LazyListState,
    searchQuery: String,
    dateFormatPreference: DateFormatPreference,
    timeFormatPreference: TimeFormatPreference,
    onContactClick: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onAcceptFriendRequest: (FriendRequest) -> Unit,
    onRejectFriendRequest: (FriendRequest) -> Unit,
    onAddContactClick: () -> Unit,
    onContactInteraction: () -> Unit,
) {
    val visibleContacts = contacts

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    if (friendRequests.isEmpty() && visibleContacts.isEmpty()) {
        EmptyChatList(onAddContactClick = onAddContactClick)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
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
