package ltd.evilcorp.domain.features.chat.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.chat.model.Message

interface IMessageRepository {
    suspend fun add(message: Message)
    fun get(conversation: String): Flow<List<Message>>
    suspend fun getPending(conversation: String): List<Message>
    suspend fun setCorrelationId(id: Long, correlationId: Int)
    suspend fun delete(conversation: String)
    suspend fun deleteMessage(id: Long)
    suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long)
    suspend fun exists(conversation: String, message: String): Boolean
}
