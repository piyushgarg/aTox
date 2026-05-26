package ltd.evilcorp.domain.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.model.Message

interface IMessageRepository {
    fun add(message: Message)
    fun get(conversation: String): Flow<List<Message>>
    fun getPending(conversation: String): List<Message>
    fun setCorrelationId(id: Long, correlationId: Int)
    fun delete(conversation: String)
    fun deleteMessage(id: Long)
    fun setReceipt(conversation: String, correlationId: Int, timestamp: Long)
    fun exists(conversation: String, message: String): Boolean
}
