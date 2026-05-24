package ltd.evilcorp.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_messages")
data class GroupMessage(
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
}
