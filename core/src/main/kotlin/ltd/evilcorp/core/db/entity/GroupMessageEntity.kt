package ltd.evilcorp.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender

@Entity(tableName = "group_messages")
data class GroupMessageEntity(
    @ColumnInfo(name = "group_chat_id")
    val groupChatId: String,

    @ColumnInfo(name = "peer_id")
    val peerId: Int,

    @ColumnInfo(name = "sender_name")
    val senderName: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "sender")
    val sender: Sender,

    @ColumnInfo(name = "type")
    val type: MessageType,

    @ColumnInfo(name = "correlation_id")
    var correlationId: Int,

    @ColumnInfo(name = "timestamp")
    var timestamp: Long = 0,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0

    fun toDomain(): GroupMessage = GroupMessage(
        groupChatId = groupChatId,
        peerId = peerId,
        senderName = senderName,
        message = message,
        sender = sender,
        type = type,
        correlationId = correlationId,
        timestamp = timestamp,
        id = this@GroupMessageEntity.id
    )

    companion object {
        fun fromDomain(groupMessage: GroupMessage): GroupMessageEntity = GroupMessageEntity(
            groupChatId = groupMessage.groupChatId,
            peerId = groupMessage.peerId,
            senderName = groupMessage.senderName,
            message = groupMessage.message,
            sender = groupMessage.sender,
            type = groupMessage.type,
            correlationId = groupMessage.correlationId,
            timestamp = groupMessage.timestamp
        ).apply {
            id = groupMessage.id
        }
    }
}
