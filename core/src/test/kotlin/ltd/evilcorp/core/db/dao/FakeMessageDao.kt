package ltd.evilcorp.core.db.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.entity.MessageEntity

class FakeMessageDao : MessageDao {
    private val messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun save(message: MessageEntity) {
        if (message.id == 0L) {
            message.id = nextId++
        }
        messages.value = messages.value.filter { it.id != message.id } + message
    }

    override suspend fun saveAll(messages: List<MessageEntity>) {
        val list = this.messages.value.toMutableList()
        messages.forEach { msg ->
            if (msg.id == 0L) {
                msg.id = nextId++
            }
            list.removeAll { it.id == msg.id }
            list.add(msg)
        }
        this.messages.value = list
    }

    override fun load(conversation: String): Flow<List<MessageEntity>> {
        return messages.map { list ->
            list.filter { it.publicKey == conversation }
                .sortedByDescending { it.id }
                .take(150)
                .sortedBy { it.id }
        }
    }

    override suspend fun loadAllBlocking(): List<MessageEntity> {
        return messages.value
    }

    override suspend fun loadPending(conversation: String): List<MessageEntity> {
        return messages.value.filter { it.publicKey == conversation && it.timestamp == 0L }
    }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) {
        val list = messages.value.map { msg ->
            if (msg.id == id) {
                msg.copy().apply {
                    this.id = msg.id
                    this.correlationId = correlationId
                }
            } else {
                msg
            }
        }
        messages.value = list
    }

    override suspend fun delete(conversation: String) {
        messages.value = messages.value.filter { it.publicKey != conversation }
    }

    override suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) {
        val list = messages.value.map { msg ->
            if (msg.publicKey == conversation && msg.correlationId == correlationId && msg.timestamp == 0L) {
                msg.copy().apply {
                    this.id = msg.id
                    this.timestamp = timestamp
                }
            } else {
                msg
            }
        }
        messages.value = list
    }

    override suspend fun deleteMessage(id: Long) {
        messages.value = messages.value.filter { it.id != id }
    }

    override suspend fun exists(conversation: String, message: String): Boolean {
        return messages.value.any { it.publicKey == conversation && it.message == message }
    }
}
