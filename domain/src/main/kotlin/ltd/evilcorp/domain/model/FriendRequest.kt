package ltd.evilcorp.domain.model

data class FriendRequest(
    val publicKey: String,
    val message: String = "",
)
