package ltd.evilcorp.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ltd.evilcorp.domain.model.Group
import ltd.evilcorp.domain.model.GroupPrivacyState

@Entity(tableName = "groups")
data class GroupEntity(
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
) {
    fun toDomain(): Group = Group(
        chatId = chatId,
        name = name,
        topic = topic,
        passwordProtected = passwordProtected,
        privacyState = privacyState,
        peerCount = peerCount,
        selfPeerId = selfPeerId,
        selfRole = selfRole,
        lastMessage = lastMessage,
        hasUnreadMessages = hasUnreadMessages,
        draftMessage = draftMessage,
        connected = connected,
        groupNumber = groupNumber
    )

    companion object {
        fun fromDomain(group: Group): GroupEntity = GroupEntity(
            chatId = group.chatId,
            name = group.name,
            topic = group.topic,
            passwordProtected = group.passwordProtected,
            privacyState = group.privacyState,
            peerCount = group.peerCount,
            selfPeerId = group.selfPeerId,
            selfRole = group.selfRole,
            lastMessage = group.lastMessage,
            hasUnreadMessages = group.hasUnreadMessages,
            draftMessage = group.draftMessage,
            connected = group.connected,
            groupNumber = group.groupNumber
        )
    }
}
