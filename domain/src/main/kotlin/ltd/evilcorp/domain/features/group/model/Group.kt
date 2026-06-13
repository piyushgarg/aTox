package ltd.evilcorp.domain.features.group.model

import ltd.evilcorp.domain.core.model.Stable

enum class GroupPrivacyState {
    Public,
    Private,
}

@Stable
data class Group(
    val chatId: String,
    val name: String = "",
    val topic: String = "",
    val passwordProtected: Boolean = false,
    val privacyState: GroupPrivacyState = GroupPrivacyState.Public,
    val peerCount: Int = 0,
    val selfPeerId: Int = -1,
    val selfRole: String = "User",
    val lastMessage: Long = 0,
    val hasUnreadMessages: Boolean = false,
    val draftMessage: String = "",
    val connected: Boolean = false,
    val groupNumber: Int = -1,
)
