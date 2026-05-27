package ltd.evilcorp.atox.infrastructure.backup

import javax.inject.Inject
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.backup.BackupDataProvider
import ltd.evilcorp.domain.backup.IChatHistoryBackupHelper
import ltd.evilcorp.domain.model.Message
import org.json.JSONArray
import org.json.JSONObject

class ChatHistoryBackupDataProvider @Inject constructor(
    private val helper: IChatHistoryBackupHelper,
) : BackupDataProvider {
    override val id: String = "chat_history"
    override val displayNameRes: Int = R.string.backup_module_chat_history
    override val descriptionRes: Int = R.string.backup_module_chat_history_description

    override fun serialize(): ByteArray = serializeMessages(helper.serializeChatHistory())

    override fun deserialize(data: ByteArray) {
        helper.deserializeChatHistory(parseMessages(data))
    }
}

class CallLogBackupDataProvider @Inject constructor(
    private val helper: IChatHistoryBackupHelper,
) : BackupDataProvider {
    override val id: String = "call_log"
    override val displayNameRes: Int = R.string.backup_module_call_log
    override val descriptionRes: Int = R.string.backup_module_call_log_description

    override fun serialize(): ByteArray {
        val callMessages = helper.serializeCallLog()
        return serializeMessages(callMessages)
    }

    override fun deserialize(data: ByteArray) {
        helper.deserializeCallLog(parseMessages(data))
    }
}

private fun serializeMessages(messages: List<Message>): ByteArray {
    val entries = JSONArray()
    messages.forEach { message ->
        entries.put(JSONObject().apply {
            put("id", message.id)
            put("publicKey", message.publicKey)
            put("message", message.message)
            put("sender", message.sender.name)
            put("type", message.type.name)
            put("correlationId", message.correlationId)
            put("timestamp", message.timestamp)
        })
    }
    return JSONObject().put("messages", entries).toString().encodeToByteArray()
}

private fun parseMessages(data: ByteArray): List<Message> {
    val entries = JSONObject(data.decodeToString()).getJSONArray("messages")
    return buildList {
        for (index in 0 until entries.length()) {
            val item = entries.getJSONObject(index)
            add(Message(
                publicKey = item.getString("publicKey"),
                message = item.getString("message"),
                sender = enumValueOf(item.getString("sender")),
                type = enumValueOf(item.getString("type")),
                correlationId = item.optInt("correlationId"),
                timestamp = item.optLong("timestamp"),
            ).apply {
                id = item.optLong("id")
            })
        }
    }
}
