package ltd.evilcorp.core.repository

import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.db.dao.MessageDao
import ltd.evilcorp.core.db.entity.MessageEntity
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository

@Singleton
class MessageRepositoryImpl @Inject internal constructor(
    private val database: Database,
    private val messageDao: MessageDao,
) : IMessageRepository {
    override suspend fun add(message: Message) {
        database.withTransaction {
            messageDao.save(MessageEntity.fromDomain(message))
            database.contactDao().setLastMessage(message.publicKey, Date().time)
        }
    }

    override fun get(conversation: String): Flow<List<Message>> =
        messageDao.load(conversation).map { list -> list.map { it.toDomain() } }

    override suspend fun getPending(conversation: String): List<Message> =
        messageDao.loadPending(conversation).map { it.toDomain() }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) = messageDao.setCorrelationId(id, correlationId)

    override suspend fun delete(conversation: String) = messageDao.delete(conversation)

    override suspend fun deleteMessage(id: Long) = messageDao.deleteMessage(id)

    override suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) =
        messageDao.setReceipt(conversation, correlationId, timestamp)

    override suspend fun exists(conversation: String, message: String): Boolean =
        messageDao.exists(conversation, message)
}
