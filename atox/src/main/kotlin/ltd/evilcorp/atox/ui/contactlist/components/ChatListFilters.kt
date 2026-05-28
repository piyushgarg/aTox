package ltd.evilcorp.atox.ui.contactlist.components

import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.FriendRequest

fun visibleChatContacts(
    contacts: List<Contact>,
    searchQuery: String,
): List<Contact> {
    val sortedContacts = contacts.sortedWith(
        compareByDescending<Contact> { it.hasUnreadMessages }
            .thenByDescending { it.connectionStatus != ConnectionStatus.None }
            .thenByDescending { it.lastMessage }
            .thenBy { it.name.ifBlank { it.publicKey.take(8) }.lowercase() }
    )
    if (searchQuery.isBlank()) return sortedContacts

    val query = searchQuery.trim()
    return sortedContacts.filter {
        it.name.contains(query, ignoreCase = true) ||
            it.statusMessage.contains(query, ignoreCase = true) ||
            it.publicKey.contains(query, ignoreCase = true)
    }
}

fun chatListAttentionCount(
    contacts: List<Contact>,
    friendRequests: List<FriendRequest>,
): Int = contacts.count { it.hasUnreadMessages } + friendRequests.size
