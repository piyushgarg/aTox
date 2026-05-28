package ltd.evilcorp.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    @ColumnInfo(name = "public_key")
    val publicKey: String,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "status_message")
    var statusMessage: String = "...",

    @ColumnInfo(name = "last_message")
    var lastMessage: Long = 0,

    @ColumnInfo(name = "status")
    var status: UserStatus = UserStatus.None,

    @ColumnInfo(name = "connection_status")
    var connectionStatus: ConnectionStatus = ConnectionStatus.None,

    @ColumnInfo(name = "typing")
    var typing: Boolean = false,

    @ColumnInfo(name = "avatar_uri")
    var avatarUri: String = "",

    @ColumnInfo(name = "has_unread_messages")
    var hasUnreadMessages: Boolean = false,

    @ColumnInfo(name = "draft_message")
    var draftMessage: String = "",

    @ColumnInfo(name = "last_online")
    var lastOnline: Long = 0,
) {
    fun toDomain(): Contact = Contact(
        publicKey = publicKey,
        name = name,
        statusMessage = statusMessage,
        lastMessage = lastMessage,
        status = status,
        connectionStatus = connectionStatus,
        typing = typing,
        avatarUri = avatarUri,
        hasUnreadMessages = hasUnreadMessages,
        draftMessage = draftMessage,
        lastOnline = lastOnline
    )

    companion object {
        fun fromDomain(contact: Contact): ContactEntity = ContactEntity(
            publicKey = contact.publicKey,
            name = contact.name,
            statusMessage = contact.statusMessage,
            lastMessage = contact.lastMessage,
            status = contact.status,
            connectionStatus = contact.connectionStatus,
            typing = contact.typing,
            avatarUri = contact.avatarUri,
            hasUnreadMessages = contact.hasUnreadMessages,
            draftMessage = contact.draftMessage,
            lastOnline = contact.lastOnline
        )
    }
}
