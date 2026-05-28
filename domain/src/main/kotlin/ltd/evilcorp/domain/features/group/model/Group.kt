package ltd.evilcorp.domain.features.group.model

enum class GroupPrivacyState {
    Public,
    Private,
}

data class Group(
    val chatId: String,
    var name: String = "",
    var topic: String = "",
    var passwordProtected: Boolean = false,
    var privacyState: GroupPrivacyState = GroupPrivacyState.Public,
    var peerCount: Int = 0,
    var selfPeerId: Int = -1,
    var selfRole: String = "User",
    var lastMessage: Long = 0,
    var hasUnreadMessages: Boolean = false,
    var draftMessage: String = "",
    var connected: Boolean = false,
    var groupNumber: Int = -1,
)
