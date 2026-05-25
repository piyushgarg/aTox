package ltd.evilcorp.atox.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.repository.MessageRepository
import ltd.evilcorp.domain.model.DomainMessage
import ltd.evilcorp.domain.model.toDb
import ltd.evilcorp.domain.model.toDomain
import ltd.evilcorp.domain.repository.IChatRepository

@Singleton
class ChatRepositoryAdapter @Inject constructor(
    private val messageRepository: MessageRepository
) : IChatRepository {
    override fun add(message: DomainMessage) = messageRepository.add(message.toDb())
    override fun get(conversation: String): Flow<List<DomainMessage>> = messageRepository.get(conversation).map { list -> list.map { it.toDomain() } }
    override fun getPending(conversation: String): List<DomainMessage> = messageRepository.getPending(conversation).map { it.toDomain() }
    override fun setCorrelationId(id: Long, correlationId: Int) = messageRepository.setCorrelationId(id, correlationId)
    override fun delete(conversation: String) = messageRepository.delete(conversation)
    override fun deleteMessage(id: Long) = messageRepository.deleteMessage(id)
    override fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) = messageRepository.setReceipt(conversation, correlationId, timestamp)
    override fun exists(conversation: String, message: String): Boolean = messageRepository.exists(conversation, message)
}
