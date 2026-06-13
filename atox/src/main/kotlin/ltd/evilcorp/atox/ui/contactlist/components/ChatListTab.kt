@file:Suppress("MatchingDeclarationName")
package ltd.evilcorp.atox.ui.contactlist.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.atox.ui.navigation.LocalTabPadding
import ltd.evilcorp.atox.ui.groupchat.components.GroupItemCard
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

sealed class ChatItem(val timestamp: Long) {
    data class ContactItem(val contact: Contact) : ChatItem(contact.lastMessage)
    data class GroupItem(val group: Group) : ChatItem(group.lastMessage)
}

private val ContentPaddingTop = 4.dp
private val ContentPaddingBottomDefault = 16.dp
private val InvitePaddingHorizontal = 16.dp
private val InvitePaddingVertical = 8.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListTab(
    contacts: List<Contact>,
    groups: List<Group>,
    connectionStatuses: Map<String, GroupConnectionStatus>,
    friendRequests: List<FriendRequest>,
    groupInvite: GroupInvite?,
    groupInviteFriendName: String,
    listState: LazyListState,
    dateFormatPreference: DateFormatPreference,
    timeFormatPreference: TimeFormatPreference,
    onContactClick: (Contact) -> Unit,
    onGroupClick: (Group) -> Unit,
    onLeaveGroup: (Group) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onAcceptFriendRequest: (FriendRequest) -> Unit,
    onRejectFriendRequest: (FriendRequest) -> Unit,
    onAcceptGroupInvite: () -> Unit,
    onRejectGroupInvite: () -> Unit,
    onAddContactClick: () -> Unit,
    onContactInteraction: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val chatItems = remember(contacts, groups) {
        val items = mutableListOf<ChatItem>()
        items.addAll(contacts.map { ChatItem.ContactItem(it) })
        items.addAll(groups.map { ChatItem.GroupItem(it) })
        items.sortedByDescending { it.timestamp }
    }

    if (friendRequests.isEmpty() && chatItems.isEmpty() && groupInvite == null) {
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
            items = chatItems,
            key = { item ->
                when (item) {
                    is ChatItem.ContactItem -> "contact_${item.contact.publicKey}"
                    is ChatItem.GroupItem -> "group_${item.group.chatId}"
                }
            },
            contentType = { item ->
                when (item) {
                    is ChatItem.ContactItem -> "contact"
                    is ChatItem.GroupItem -> "group"
                }
            },
        ) { item ->
            when (item) {
                is ChatItem.ContactItem -> {
                    ContactItemCard(
                        contact = item.contact,
                        dateFormatPreference = dateFormatPreference,
                        timeFormatPreference = timeFormatPreference,
                        onClick = {
                            onContactInteraction()
                            onContactClick(item.contact)
                        },
                        onDelete = { onDeleteContact(item.contact) },
                        modifier = Modifier
                    )
                }
                is ChatItem.GroupItem -> {
                    GroupItemCard(
                        group = item.group,
                        connectionStatus = connectionStatuses[item.group.chatId] ?: GroupConnectionStatus.Disconnected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGroupClick(item.group)
                        },
                        onLongClick = {
                            onLeaveGroup(item.group)
                        }
                    )
                }
            }
        }
    }
}
