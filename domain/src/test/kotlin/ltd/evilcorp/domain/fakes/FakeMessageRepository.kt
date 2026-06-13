package ltd.evilcorp.domain.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository

class FakeMessageRepository : IMessageRepository {
    private val messages = MutableStateFlow<List<Message>>(emptyList())

    override suspend fun add(message: Message) {
        messages.value = messages.value + message
    }

    override fun get(conversation: String): Flow<List<Message>> {
        return messages.map { list -> list.filter { it.publicKey == conversation } }
    }

    override suspend fun getPending(conversation: String): List<Message> {
        return messages.value.filter { it.publicKey == conversation && it.timestamp == 0L }
    }

    override suspend fun getPaged(conversation: String, limit: Int, offset: Int): List<Message> {
        val list = messages.value.filter { it.publicKey == conversation }.sortedByDescending { it.id }
        if (offset >= list.size) return emptyList()
        return list.drop(offset).take(limit)
    }

    override fun getPagingFlow(conversation: String): Flow<androidx.paging.PagingData<Message>> {
        return androidx.paging.Pager(
            config = androidx.paging.PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                object : androidx.paging.PagingSource<Int, Message>() {
                    override fun getRefreshKey(state: androidx.paging.PagingState<Int, Message>): Int? {
                        return state.anchorPosition
                    }

                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Message> {
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
        ).flow
    }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) {
        messages.value = messages.value.map { msg ->
            if (msg.id == id) msg.copy(correlationId = correlationId) else msg
        }
    }

    override suspend fun delete(conversation: String) {
        messages.value = messages.value.filter { it.publicKey != conversation }
    }

    override suspend fun deleteMessage(id: Long) {
        messages.value = messages.value.filter { it.id != id }
    }

    override suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) {
        messages.value = messages.value.map { msg ->
            if (msg.publicKey == conversation && msg.correlationId == correlationId) {
                msg.copy(timestamp = timestamp)
            } else {
                msg
            }
        }
    }

    override suspend fun exists(conversation: String, message: String): Boolean {
        return messages.value.any { it.publicKey == conversation && it.message == message }
    }
}
