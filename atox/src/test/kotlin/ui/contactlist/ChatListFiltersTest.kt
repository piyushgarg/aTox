package ui.contactlist

import kotlin.test.Test
import kotlin.test.assertEquals
import ltd.evilcorp.atox.ui.contactlist.components.chatListAttentionCount
import ltd.evilcorp.atox.ui.contactlist.components.visibleChatContacts
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.FriendRequest

class ChatListFiltersTest {
    @Test
    fun `visible contacts prioritizes unread before online and recency`() {
        val oldUnreadOffline = contact(
            publicKey = "unread-offline",
            name = "Unread Offline",
            lastMessage = 1,
            hasUnreadMessages = true,
        )
        val freshOnline = contact(
            publicKey = "fresh-online",
            name = "Fresh Online",
            lastMessage = 100,
            connectionStatus = ConnectionStatus.TCP,
        )
        val staleOnline = contact(
            publicKey = "stale-online",
            name = "Stale Online",
            lastMessage = 10,
            connectionStatus = ConnectionStatus.UDP,
        )
        val freshOffline = contact(
            publicKey = "fresh-offline",
            name = "Fresh Offline",
            lastMessage = 200,
        )

        val result = visibleChatContacts(
            contacts = listOf(freshOffline, staleOnline, freshOnline, oldUnreadOffline),
            searchQuery = "",
        )

        assertEquals(
            listOf("unread-offline", "fresh-online", "stale-online", "fresh-offline"),
            result.map { it.publicKey },
        )
    }

    @Test
    fun `visible contacts sorts equal priority by display name fallback`() {
        val beta = contact(publicKey = "bbbbbbbb", name = "Beta")
        val unnamed = contact(publicKey = "aaaaaaaa", name = "")
        val alpha = contact(publicKey = "cccccccc", name = "Alpha")

        val result = visibleChatContacts(
            contacts = listOf(beta, unnamed, alpha),
            searchQuery = "",
        )

        assertEquals(listOf("aaaaaaaa", "cccccccc", "bbbbbbbb"), result.map { it.publicKey })
    }

    @Test
    fun `visible contacts filters by name status and public key`() {
        val byName = contact(publicKey = "key-1", name = "Sergey")
        val byStatus = contact(publicKey = "key-2", name = "Other", statusMessage = "Working remotely")
        val byPublicKey = contact(publicKey = "abcdef123456", name = "No match")
        val hidden = contact(publicKey = "hidden", name = "Hidden")

        assertEquals(
            listOf("key-1"),
            visibleChatContacts(listOf(hidden, byName), "serg").map { it.publicKey },
        )
        assertEquals(
            listOf("key-2"),
            visibleChatContacts(listOf(hidden, byStatus), "remote").map { it.publicKey },
        )
        assertEquals(
            listOf("abcdef123456"),
            visibleChatContacts(listOf(hidden, byPublicKey), "DEF123").map { it.publicKey },
        )
    }

    @Test
    fun `attention count includes unread chats and friend requests`() {
        val contacts = listOf(
            contact(publicKey = "read"),
            contact(publicKey = "unread-1", hasUnreadMessages = true),
            contact(publicKey = "unread-2", hasUnreadMessages = true),
        )
        val requests = listOf(
            FriendRequest(publicKey = "request-1"),
            FriendRequest(publicKey = "request-2"),
        )

        assertEquals(4, chatListAttentionCount(contacts, requests))
    }

    private fun contact(
        publicKey: String,
        name: String = publicKey,
        statusMessage: String = "",
        lastMessage: Long = 0,
        connectionStatus: ConnectionStatus = ConnectionStatus.None,
        hasUnreadMessages: Boolean = false,
    ): Contact = Contact(
        publicKey = publicKey,
        name = name,
        statusMessage = statusMessage,
        lastMessage = lastMessage,
        connectionStatus = connectionStatus,
        hasUnreadMessages = hasUnreadMessages,
    )
}
