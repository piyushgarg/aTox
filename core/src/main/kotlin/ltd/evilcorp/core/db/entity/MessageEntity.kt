package ltd.evilcorp.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender

@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversation", "id"])],
)
data class MessageEntity(
    @ColumnInfo(name = "conversation")
    val publicKey: String,

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

    fun toDomain(): Message = Message(
        publicKey = publicKey,
        message = message,
        sender = sender,
        type = type,
        correlationId = correlationId,
        timestamp = timestamp
    ).apply {
        id = this@MessageEntity.id
    }

    companion object {
        fun fromDomain(message: Message): MessageEntity = MessageEntity(
            publicKey = message.publicKey,
            message = message.message,
            sender = message.sender,
            type = message.type,
            correlationId = message.correlationId,
            timestamp = message.timestamp
        ).apply {
            id = message.id
        }
    }
}
