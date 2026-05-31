package ltd.evilcorp.core.db.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.entity.GroupMessageEntity

class FakeGroupMessageDao : GroupMessageDao {
    private val messages = MutableStateFlow<List<GroupMessageEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun save(message: GroupMessageEntity) {
        if (message.id == 0L) {
            message.id = nextId++
        }
        messages.value = messages.value.filter { it.id != message.id } + message
    }

    override fun load(groupChatId: String): Flow<List<GroupMessageEntity>> {
        return messages.map { list ->
            list.filter { it.groupChatId == groupChatId }.sortedBy { it.id }
        }
    }

    override fun loadPending(groupChatId: String): List<GroupMessageEntity> {
        return messages.value.filter { it.groupChatId == groupChatId && it.timestamp == 0L }
    }

    override fun loadUnsent(groupChatId: String): List<GroupMessageEntity> {
        // sender == Sent (value 0 usually, or matching Sender enum ordinal)
        // type != MessageType.GroupEvent (value 3 usually)
        // correlation_id == -1
        return messages.value.filter {
            it.groupChatId == groupChatId &&
            it.correlationId == -1 &&
            it.sender.ordinal == 0 &&
            it.type.ordinal != 3
        }
    }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) {
        messages.value = messages.value.map { msg ->
            if (msg.id == id) {
                msg.copy().apply {
                    this.id = msg.id
                    this.correlationId = correlationId
                }
            } else {
                msg
            }
        }
    }

    override suspend fun delete(groupChatId: String) {
        messages.value = messages.value.filter { it.groupChatId != groupChatId }
    }

    override suspend fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long) {
        messages.value = messages.value.map { msg ->
            if (msg.groupChatId == groupChatId && msg.correlationId == correlationId && msg.timestamp == 0L) {
                msg.copy().apply {
                    this.id = msg.id
                    this.timestamp = timestamp
                }
            } else {
                msg
            }
        }
    }

    override fun existsByCorrelationId(groupChatId: String, correlationId: Int): Int {
        return if (messages.value.any { it.groupChatId == groupChatId && it.correlationId == correlationId }) 1 else 0
    }

    override fun getMessageIds(groupChatId: String): List<Int> {
        return messages.value.filter { it.groupChatId == groupChatId }.map { it.correlationId }
    }

    override fun getMessagesByIds(groupChatId: String, ids: Set<Int>): List<GroupMessageEntity> {
        return messages.value.filter { it.groupChatId == groupChatId && it.correlationId in ids }
    }

    override suspend fun deleteMessage(id: Long) {
        messages.value = messages.value.filter { it.id != id }
    }

    fun loadAllBlocking(): List<GroupMessageEntity> {
        return messages.value
    }
}
