package ltd.evilcorp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.entity.GroupMessageEntity

@Dao
@Suppress("ComplexInterface")
interface GroupMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(message: GroupMessageEntity)

    @Query("SELECT * FROM group_messages WHERE group_chat_id = :groupChatId ORDER BY id ASC")
    fun load(groupChatId: String): Flow<List<GroupMessageEntity>>

    @Query("SELECT * FROM group_messages WHERE group_chat_id = :groupChatId AND timestamp = 0")
    fun loadPending(groupChatId: String): List<GroupMessageEntity>

    @Query("SELECT * FROM group_messages WHERE group_chat_id = :groupChatId AND correlation_id = -1 AND sender = 0 AND type != 3")
    fun loadUnsent(groupChatId: String): List<GroupMessageEntity>

    @Query("UPDATE group_messages SET correlation_id = :correlationId WHERE id = :id")
    suspend fun setCorrelationId(id: Long, correlationId: Int)

    @Query("DELETE FROM group_messages WHERE group_chat_id = :groupChatId")
    suspend fun delete(groupChatId: String)

    @Query("UPDATE group_messages SET timestamp = :timestamp WHERE group_chat_id = :groupChatId AND correlation_id = :correlationId AND timestamp = 0")
    suspend fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long)

    @Query("SELECT COUNT(*) FROM group_messages WHERE group_chat_id = :groupChatId AND correlation_id = :correlationId")
    fun existsByCorrelationId(groupChatId: String, correlationId: Int): Int

    @Query("SELECT correlation_id FROM group_messages WHERE group_chat_id = :groupChatId")
    fun getMessageIds(groupChatId: String): List<Int>

    @Query("SELECT * FROM group_messages WHERE group_chat_id = :groupChatId AND correlation_id IN (:ids)")
    fun getMessagesByIds(groupChatId: String, ids: Set<Int>): List<GroupMessageEntity>

    @Query("DELETE FROM group_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)
}
