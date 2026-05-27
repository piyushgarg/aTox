package ltd.evilcorp.domain.model

// These enums are 1:1 mappings of the Tox protocol connection/user status values.
enum class ConnectionStatus {
    None,
    TCP,
    UDP,
}

enum class UserStatus {
    None,
    Away,
    Busy,
}

@Stable
data class Contact(
    val publicKey: String,
    var name: String = "",
    var statusMessage: String = "...",
    var lastMessage: Long = 0,
    var status: UserStatus = UserStatus.None,
    var connectionStatus: ConnectionStatus = ConnectionStatus.None,
    var typing: Boolean = false,
    var avatarUri: String = "",
    var hasUnreadMessages: Boolean = false,
    var draftMessage: String = "",
    var lastOnline: Long = 0,
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
