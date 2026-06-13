package ltd.evilcorp.core.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map as pagingMap
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
    private val dbProvider: javax.inject.Provider<Database>? = null
) : IMessageRepository {
    private val activeDatabase: Database get() = dbProvider?.get() ?: database
    private val activeMessageDao: MessageDao get() = activeDatabase.messageDao()

    override suspend fun add(message: Message) {
        activeDatabase.withTransaction {
            activeMessageDao.save(MessageEntity.fromDomain(message))
            activeDatabase.contactDao().setLastMessage(message.publicKey, Date().time)
        }
    }

    override fun get(conversation: String): Flow<List<Message>> =
        activeMessageDao.load(conversation).map { list -> list.map { it.toDomain() } }

    override suspend fun getPaged(conversation: String, limit: Int, offset: Int): List<Message> =
        activeMessageDao.loadConversationPaged(conversation, limit, offset).map { it.toDomain() }

    override fun getPagingFlow(conversation: String): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { activeMessageDao.loadConversationPagingSource(conversation) }
        ).flow.map { pagingData ->
            pagingData.pagingMap { it.toDomain() }
        }
    }

    override suspend fun getPending(conversation: String): List<Message> =
        activeMessageDao.loadPending(conversation).map { it.toDomain() }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) = activeMessageDao.setCorrelationId(id, correlationId)

    override suspend fun delete(conversation: String) = activeMessageDao.delete(conversation)

    override suspend fun deleteMessage(id: Long) = activeMessageDao.deleteMessage(id)

    override suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) =
        activeMessageDao.setReceipt(conversation, correlationId, timestamp)

    override suspend fun exists(conversation: String, message: String): Boolean =
        activeMessageDao.exists(conversation, message)
}
