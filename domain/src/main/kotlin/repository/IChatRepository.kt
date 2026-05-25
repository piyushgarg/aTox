package ltd.evilcorp.domain.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.model.DomainMessage

interface IChatRepository {
    fun add(message: DomainMessage)
    fun get(conversation: String): Flow<List<DomainMessage>>
    fun getPending(conversation: String): List<DomainMessage>
    fun setCorrelationId(id: Long, correlationId: Int)
    fun delete(conversation: String)
    fun deleteMessage(id: Long)
    fun setReceipt(conversation: String, correlationId: Int, timestamp: Long)
    fun exists(conversation: String, message: String): Boolean
}
