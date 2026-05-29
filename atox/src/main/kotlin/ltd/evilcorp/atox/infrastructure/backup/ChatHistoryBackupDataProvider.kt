package ltd.evilcorp.atox.infrastructure.backup

import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.features.backup.repository.IChatHistoryBackupHelper
import ltd.evilcorp.domain.features.chat.model.Message

class ChatHistoryBackupDataProvider @Inject constructor(
    private val helper: IChatHistoryBackupHelper,
) : IBackupDataProvider {
    override val id: String = "chat_history"
    override val displayNameRes: Int = R.string.backup_module_chat_history
    override val descriptionRes: Int = R.string.backup_module_chat_history_description

    override suspend fun serialize(): ByteArray = serializeMessages(helper.serializeChatHistory())

    override suspend fun deserialize(data: ByteArray) {
        helper.deserializeChatHistory(parseMessages(data))
    }
}

class CallLogBackupDataProvider @Inject constructor(
    private val helper: IChatHistoryBackupHelper,
) : IBackupDataProvider {
    override val id: String = "call_log"
    override val displayNameRes: Int = R.string.backup_module_call_log
    override val descriptionRes: Int = R.string.backup_module_call_log_description

    override suspend fun serialize(): ByteArray {
        val callMessages = helper.serializeCallLog()
        return serializeMessages(callMessages)
    }

    override suspend fun deserialize(data: ByteArray) {
        helper.deserializeCallLog(parseMessages(data))
    }
}

@Serializable
private data class MessageBackupPayload(
    val id: Long,
    val publicKey: String,
    val message: String,
    val sender: String,
    val type: String,
    val correlationId: Int,
    val timestamp: Long
)

@Serializable
private data class MessagesBackupContainer(
    val messages: List<MessageBackupPayload>
)

private fun serializeMessages(messages: List<Message>): ByteArray {
    val container = MessagesBackupContainer(
        messages = messages.map { message ->
            MessageBackupPayload(
                id = message.id,
                publicKey = message.publicKey,
                message = message.message,
                sender = message.sender.name,
                type = message.type.name,
                correlationId = message.correlationId,
                timestamp = message.timestamp
            )
        }
    )
    return Json.encodeToString(container).encodeToByteArray()
}

private fun parseMessages(data: ByteArray): List<Message> {
    val container = Json.decodeFromString<MessagesBackupContainer>(data.decodeToString())
    return container.messages.map { item ->
        Message(
            publicKey = item.publicKey,
            message = item.message,
            sender = enumValueOf(item.sender),
            type = enumValueOf(item.type),
            correlationId = item.correlationId,
            timestamp = item.timestamp,
        ).apply {
            id = item.id
        }
    }
}
