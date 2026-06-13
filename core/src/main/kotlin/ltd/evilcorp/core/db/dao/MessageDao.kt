package ltd.evilcorp.core.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.entity.MessageEntity

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM (SELECT * FROM messages WHERE conversation == :conversation ORDER BY id DESC LIMIT 150) ORDER BY id ASC")
    fun load(conversation: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversation == :conversation ORDER BY id DESC")
    fun loadConversationPagingSource(conversation: String): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages")
    suspend fun loadAllBlocking(): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun loadPaged(limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversation == :conversation ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun loadConversationPaged(conversation: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE correlation_id == -2147483648 ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun loadCallLogPaged(limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversation == :conversation AND timestamp == 0")
    suspend fun loadPending(conversation: String): List<MessageEntity>

    @Query("UPDATE messages SET correlation_id = :correlationId WHERE id == :id")
    suspend fun setCorrelationId(id: Long, correlationId: Int)

    @Query("DELETE FROM messages WHERE conversation == :conversation")
    suspend fun delete(conversation: String)

    @Query(
        """
        UPDATE messages SET timestamp = :timestamp 
        WHERE conversation == :conversation 
        AND correlation_id == :correlationId 
        AND timestamp == 0
        """
    )
    suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE conversation == :conversation AND message == :message LIMIT 1)")
    suspend fun exists(conversation: String, message: String): Boolean
}
