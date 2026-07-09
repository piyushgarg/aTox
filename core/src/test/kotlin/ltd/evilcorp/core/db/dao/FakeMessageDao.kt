package ltd.evilcorp.core.db.dao

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.entity.MessageEntity

class FakeMessageDao : MessageDao {
    private val messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    private var nextId = 1L

    override fun loadConversationPagingSource(conversation: String): PagingSource<Int, MessageEntity> {
        return object : PagingSource<Int, MessageEntity>() {
            override fun getRefreshKey(state: androidx.paging.PagingState<Int, MessageEntity>): Int? {
                return state.anchorPosition
            }

            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MessageEntity> {
                val list = messages.value.filter { it.publicKey == conversation }.sortedByDescending { it.id }
                val key = params.key ?: 0
                val loadSize = params.loadSize
                if (key >= list.size) {
                    return LoadResult.Page(
                        data = emptyList(),
                        prevKey = if (key > 0) key - loadSize else null,
                        nextKey = null
                    )
                }
                val data = list.drop(key).take(loadSize)
                return LoadResult.Page(
                    data = data,
                    prevKey = if (key > 0) key - loadSize else null,
                    nextKey = if (key + loadSize < list.size) key + loadSize else null
                )
            }
        }
    }

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

    override suspend fun loadAll(): List<MessageEntity> {
        return messages.value
    }

    override suspend fun loadPaged(limit: Int, offset: Int): List<MessageEntity> {
        val list = messages.value.sortedBy { it.id }
        if (offset >= list.size) return emptyList()
        return list.drop(offset).take(limit)
    }

    override suspend fun loadConversationPaged(conversation: String, limit: Int, offset: Int): List<MessageEntity> {
        val list = messages.value.filter { it.publicKey == conversation }.sortedByDescending { it.id }
        if (offset >= list.size) return emptyList()
        return list.drop(offset).take(limit)
    }

    override suspend fun loadCallLogPaged(limit: Int, offset: Int): List<MessageEntity> {
        val list = messages.value.filter { it.correlationId == -2147483648 }.sortedBy { it.id }
        if (offset >= list.size) return emptyList()
        return list.drop(offset).take(limit)
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
