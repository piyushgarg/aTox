package ltd.evilcorp.domain.model

import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.UserStatus

data class DomainUser(
    val publicKey: String,
    var name: String = "aTox user",
    var statusMessage: String = "Brought to you live, by aTox",
    var status: UserStatus = UserStatus.None,
    var connectionStatus: ConnectionStatus = ConnectionStatus.None,
    var password: String = "",
)
