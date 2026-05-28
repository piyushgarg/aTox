package ltd.evilcorp.domain.features.contacts.model

data class FriendRequest(
    val publicKey: String,
    val message: String = "",
)
