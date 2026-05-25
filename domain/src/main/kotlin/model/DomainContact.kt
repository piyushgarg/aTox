package ltd.evilcorp.domain.model

import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.UserStatus

data class DomainContact(
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
