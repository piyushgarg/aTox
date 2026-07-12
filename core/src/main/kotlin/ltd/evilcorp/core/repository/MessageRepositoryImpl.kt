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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.room.withTransaction
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.db.dao.MessageDao
import ltd.evilcorp.core.db.entity.MessageEntity
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.core.profile.ProfileManager
import android.content.Context

@Singleton
class MessageRepositoryImpl @Inject internal constructor(
    private val database: Database,
    private val dbProvider: javax.inject.Provider<Database>? = null,
    private val context: Context? = null
) : IMessageRepository {
    private val activeDatabase: Database get() = dbProvider?.get() ?: database
    private val activeMessageDao: MessageDao get() = activeDatabase.messageDao()
    private val profileIdFlow = MutableStateFlow(context?.let { ProfileManager.getActiveProfileId(it) } ?: ProfileManager.DEFAULT_PROFILE_ID)

    init {
        // Update profileIdFlow when needed (will be triggered by cache access in other methods)
        if (context != null) {
            profileIdFlow.value = ProfileManager.getActiveProfileId(context)
        }
    }

    private fun updateProfileIdIfNeeded() {
        if (context != null) {
            val currentProfileId = ProfileManager.getActiveProfileId(context)
            if (profileIdFlow.value != currentProfileId) {
                profileIdFlow.value = currentProfileId
            }
        }
    }

    override suspend fun add(message: Message) {
        updateProfileIdIfNeeded()
        activeDatabase.withTransaction {
            activeMessageDao.save(MessageEntity.fromDomain(message))
            activeDatabase.contactDao().setLastMessage(message.publicKey, Date().time)
        }
    }

    override suspend fun addAll(messages: List<Message>) {
        if (messages.isEmpty()) return
        updateProfileIdIfNeeded()
        activeDatabase.withTransaction {
            activeMessageDao.saveAll(messages.map { MessageEntity.fromDomain(it) })
            activeDatabase.contactDao().setLastMessage(
                messages.last().publicKey,
                messages.last().timestamp.takeIf { it > 0L } ?: Date().time,
            )
        }
    }

    override fun get(conversation: String): Flow<List<Message>> = profileIdFlow
        .flatMapLatest {
            activeMessageDao.load(conversation).map { list -> list.map { it.toDomain() } }
        }

    override suspend fun getPaged(conversation: String, limit: Int, offset: Int): List<Message> {
        updateProfileIdIfNeeded()
        return activeMessageDao.loadConversationPaged(conversation, limit, offset).map { it.toDomain() }
    }

    override fun getPagingFlow(conversation: String): Flow<PagingData<Message>> = profileIdFlow
        .flatMapLatest {
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { activeMessageDao.loadConversationPagingSource(conversation) }
            ).flow.map { pagingData ->
                pagingData.pagingMap { it.toDomain() }
            }
        }

    override suspend fun getPending(conversation: String): List<Message> {
        updateProfileIdIfNeeded()
        return activeMessageDao.loadPending(conversation).map { it.toDomain() }
    }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) {
        updateProfileIdIfNeeded()
        activeMessageDao.setCorrelationId(id, correlationId)
    }

    override suspend fun delete(conversation: String) {
        updateProfileIdIfNeeded()
        activeMessageDao.delete(conversation)
    }

    override suspend fun deleteMessage(id: Long) {
        updateProfileIdIfNeeded()
        activeMessageDao.deleteMessage(id)
    }

    override suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) {
        updateProfileIdIfNeeded()
        activeMessageDao.setReceipt(conversation, correlationId, timestamp)
    }

    override suspend fun exists(conversation: String, message: String): Boolean {
        updateProfileIdIfNeeded()
        return activeMessageDao.exists(conversation, message)
    }
}
