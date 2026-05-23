package ltd.evilcorp.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.model.GroupMessage

@Dao
interface GroupMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(message: GroupMessage)

    @Query("SELECT * FROM group_messages WHERE group_chat_id = :groupChatId ORDER BY id ASC")
    fun load(groupChatId: String): Flow<List<GroupMessage>>

    @Query("SELECT * FROM group_messages WHERE group_chat_id = :groupChatId AND timestamp = 0")
    fun loadPending(groupChatId: String): List<GroupMessage>

    @Query("SELECT * FROM group_messages WHERE group_chat_id = :groupChatId AND correlation_id = -1 AND sender = 0 AND type != 3")
    fun loadUnsent(groupChatId: String): List<GroupMessage>

    @Query("UPDATE group_messages SET correlation_id = :correlationId WHERE id = :id")
    fun setCorrelationId(id: Long, correlationId: Int)

    @Query("DELETE FROM group_messages WHERE group_chat_id = :groupChatId")
    fun delete(groupChatId: String)

    @Query("UPDATE group_messages SET timestamp = :timestamp WHERE group_chat_id = :groupChatId AND correlation_id = :correlationId AND timestamp = 0")
    fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long)

    @Query("SELECT COUNT(*) FROM group_messages WHERE group_chat_id = :groupChatId AND correlation_id = :correlationId")
    fun existsByCorrelationId(groupChatId: String, correlationId: Int): Int

    @Query("DELETE FROM group_messages WHERE id = :id")
    fun deleteMessage(id: Long)
}
