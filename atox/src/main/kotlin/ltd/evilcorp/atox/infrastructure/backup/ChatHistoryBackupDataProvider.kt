package ltd.evilcorp.atox.infrastructure.backup

import javax.inject.Inject
import ltd.evilcorp.atox.R
import ltd.evilcorp.core.db.MessageDao
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.backup.BackupDataProvider
import org.json.JSONArray
import org.json.JSONObject

class ChatHistoryBackupDataProvider @Inject constructor(
    private val messageDao: MessageDao,
) : BackupDataProvider {
    override val id: String = "chat_history"
    override val displayNameRes: Int = R.string.backup_module_chat_history
    override val descriptionRes: Int = R.string.backup_module_chat_history_description

    override fun serialize(): ByteArray = serializeMessages(messageDao.loadAllBlocking())

    override fun deserialize(data: ByteArray) {
        messageDao.saveAll(parseMessages(data))
    }
}

class CallLogBackupDataProvider @Inject constructor(
    private val messageDao: MessageDao,
) : BackupDataProvider {
    override val id: String = "call_log"
    override val displayNameRes: Int = R.string.backup_module_call_log
    override val descriptionRes: Int = R.string.backup_module_call_log_description

    override fun serialize(): ByteArray {
        val callMessages = messageDao.loadAllBlocking().filter { it.correlationId == Int.MIN_VALUE }
        return serializeMessages(callMessages)
    }

    override fun deserialize(data: ByteArray) {
        messageDao.saveAll(parseMessages(data))
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
