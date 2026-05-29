package ltd.evilcorp.domain.features.auth.model
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus

data class User(
    val publicKey: String,
    val name: String = "aTox user",
    val statusMessage: String = "Brought to you live, by aTox",
    val status: UserStatus = UserStatus.None,
    val connectionStatus: ConnectionStatus = ConnectionStatus.None,
    val password: String = "",
)

val User.initials: String
    get() {
        val displayName = name.ifEmpty { "aTox user" }
        val segments = displayName.split(" ").filter { it.isNotBlank() }
        return when {
            segments.isEmpty() -> "U"
            segments.size == 1 -> segments.first().take(1)
            else -> segments.first().take(1) + segments[1].take(1)
        }
    }
