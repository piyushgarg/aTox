package ltd.evilcorp.domain.features.chat.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.chat.model.Message

@Suppress("ComplexInterface")
interface IMessageRepository {
    suspend fun add(message: Message)
    fun get(conversation: String): Flow<List<Message>>
    suspend fun getPaged(conversation: String, limit: Int, offset: Int): List<Message>
    fun getPagingFlow(conversation: String): Flow<PagingData<Message>>
    suspend fun getPending(conversation: String): List<Message>
    suspend fun setCorrelationId(id: Long, correlationId: Int)
    suspend fun delete(conversation: String)
    suspend fun deleteMessage(id: Long)
    suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long)
    suspend fun exists(conversation: String, message: String): Boolean
}
