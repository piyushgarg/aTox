package ltd.evilcorp.domain.features.group.model
import ltd.evilcorp.domain.features.contacts.model.UserStatus

import ltd.evilcorp.domain.core.model.Stable

@Stable
data class GroupPeer(
    val groupChatId: String,
    val peerId: Int,
    var name: String = "",
    var publicKey: String = "",
    var role: String = "User",
    var isOurselves: Boolean = false,
    var status: UserStatus = UserStatus.None,
)
