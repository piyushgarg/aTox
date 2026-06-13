package ltd.evilcorp.domain.features.group.model
import ltd.evilcorp.domain.features.contacts.model.UserStatus

import ltd.evilcorp.domain.core.model.Stable

@Stable
data class GroupPeer(
    val groupChatId: String,
    val peerId: Int,
    val name: String = "",
    val publicKey: String = "",
    val role: String = "User",
    val isOurselves: Boolean = false,
    val status: UserStatus = UserStatus.None,
) {
    val colorIndex: Int
        get() = (name.hashCode().let { if (it < 0) -it else it } % 8)
}

