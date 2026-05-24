package ltd.evilcorp.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GroupPrivacyState {
    Public,
    Private,
}

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey
    @ColumnInfo(name = "chat_id")
    val chatId: String,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "topic")
    var topic: String = "",

    @ColumnInfo(name = "password_protected")
    var passwordProtected: Boolean = false,

    @ColumnInfo(name = "privacy_state")
    var privacyState: GroupPrivacyState = GroupPrivacyState.Public,

    @ColumnInfo(name = "peer_count")
    var peerCount: Int = 0,

    @ColumnInfo(name = "self_peer_id")
    var selfPeerId: Int = -1,

    @ColumnInfo(name = "self_role")
    var selfRole: String = "User",

    @ColumnInfo(name = "last_message")
    var lastMessage: Long = 0,

    @ColumnInfo(name = "has_unread_messages")
    var hasUnreadMessages: Boolean = false,

    @ColumnInfo(name = "draft_message")
    var draftMessage: String = "",

    @ColumnInfo(name = "connected")
    var connected: Boolean = false,

    @ColumnInfo(name = "group_number")
    var groupNumber: Int = -1,
)
