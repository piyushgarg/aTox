package ltd.evilcorp.core.repository

import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.MessageDao
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.domain.repository.IMessageRepository

@Singleton
class MessageRepository @Inject internal constructor(
    private val messageDao: MessageDao,
    private val contactRepository: ContactRepository,
) : IMessageRepository {
    override fun add(message: Message) {
        messageDao.save(message)
        contactRepository.setLastMessage(message.publicKey, Date().time)
    }

    override fun get(conversation: String): Flow<List<Message>> = messageDao.load(conversation)

    override fun getPending(conversation: String): List<Message> = messageDao.loadPending(conversation)

    override fun setCorrelationId(id: Long, correlationId: Int) = messageDao.setCorrelationId(id, correlationId)

    override fun delete(conversation: String) = messageDao.delete(conversation)

    override fun deleteMessage(id: Long) = messageDao.deleteMessage(id)

    override fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) =
        messageDao.setReceipt(conversation, correlationId, timestamp)

    override fun exists(conversation: String, message: String): Boolean =
        messageDao.exists(conversation, message)
}
