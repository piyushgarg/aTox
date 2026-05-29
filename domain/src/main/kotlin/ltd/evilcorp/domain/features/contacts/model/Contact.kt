package ltd.evilcorp.domain.features.contacts.model

import ltd.evilcorp.domain.core.model.Stable

// These enums are 1:1 mappings of the Tox protocol connection/user status values.
enum class ConnectionStatus(val id: Int) {
    None(0),
    TCP(1),
    UDP(2);

    companion object {
        fun fromId(id: Int): ConnectionStatus = entries.find { it.id == id } ?: None
    }
}

enum class UserStatus(val id: Int) {
    None(0),
    Away(1),
    Busy(2);

    companion object {
        fun fromId(id: Int): UserStatus = entries.find { it.id == id } ?: None
    }
}

@Stable
data class Contact(
    val publicKey: String,
    val name: String = "",
    val statusMessage: String = "...",
    val lastMessage: Long = 0,
    val status: UserStatus = UserStatus.None,
    val connectionStatus: ConnectionStatus = ConnectionStatus.None,
    val typing: Boolean = false,
    val avatarUri: String = "",
    val hasUnreadMessages: Boolean = false,
    val draftMessage: String = "",
    val lastOnline: Long = 0,
)

val Contact.initials: String
    get() {
        val displayName = name.ifEmpty { "Contact" }
        val segments = displayName.split(" ").filter { it.isNotBlank() }
        return when {
            segments.isEmpty() -> "C"
            segments.size == 1 -> segments.first().take(1)
            else -> segments.first().take(1) + segments[1].take(1)
        }
    }
