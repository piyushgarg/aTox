package ltd.evilcorp.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.model.Message

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAll(messages: List<Message>)

    @Query("SELECT * FROM messages WHERE conversation == :conversation ORDER BY id ASC")
    fun load(conversation: String): Flow<List<Message>>

    @Query("SELECT * FROM messages")
    fun loadAllBlocking(): List<Message>

    @Query("SELECT * FROM messages WHERE conversation == :conversation AND timestamp == 0")
    fun loadPending(conversation: String): List<Message>

    @Query("UPDATE messages SET correlation_id = :correlationId WHERE id == :id")
    fun setCorrelationId(id: Long, correlationId: Int)

    @Query("DELETE FROM messages WHERE conversation == :conversation")
    fun delete(conversation: String)

    @Suppress("ktlint:standard:max-line-length")
    @Query(
        "UPDATE messages SET timestamp = :timestamp WHERE conversation == :conversation AND correlation_id == :correlationId AND timestamp == 0",
    )
    fun setReceipt(conversation: String, correlationId: Int, timestamp: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    fun deleteMessage(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE conversation == :conversation AND message == :message LIMIT 1)")
    fun exists(conversation: String, message: String): Boolean
}
