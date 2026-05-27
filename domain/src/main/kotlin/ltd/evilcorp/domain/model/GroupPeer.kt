package ltd.evilcorp.domain.model

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
