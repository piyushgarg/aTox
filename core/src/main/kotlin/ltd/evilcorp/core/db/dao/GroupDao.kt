package ltd.evilcorp.core.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.entity.GroupEntity
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(group: GroupEntity)

    @Update
    suspend fun update(group: GroupEntity)

    @Delete
    suspend fun delete(group: GroupEntity)

    @Query("SELECT * FROM groups WHERE chat_id = :chatId")
    fun load(chatId: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE chat_id = :chatId")
    suspend fun loadDirect(chatId: String): GroupEntity?

    @Query("SELECT COUNT(*) FROM groups WHERE chat_id = :chatId")
    suspend fun exists(chatId: String): Int

    @Query("SELECT * FROM groups")
    fun loadAll(): Flow<List<GroupEntity>>

    @Query("UPDATE groups SET name = :name WHERE chat_id = :chatId")
    suspend fun setName(chatId: String, name: String)

    @Query("UPDATE groups SET topic = :topic WHERE chat_id = :chatId")
    suspend fun setTopic(chatId: String, topic: String)

    @Query("UPDATE groups SET password_protected = :protected WHERE chat_id = :chatId")
    suspend fun setPasswordProtected(chatId: String, protected: Boolean)

    @Query("UPDATE groups SET privacy_state = :privacyState WHERE chat_id = :chatId")
    suspend fun setPrivacyState(chatId: String, privacyState: GroupPrivacyState)

    @Query("UPDATE groups SET peer_count = :peerCount WHERE chat_id = :chatId")
    suspend fun setPeerCount(chatId: String, peerCount: Int)

    @Query("UPDATE groups SET self_peer_id = :peerId WHERE chat_id = :chatId")
    suspend fun setSelfPeerId(chatId: String, peerId: Int)

    @Query("UPDATE groups SET self_role = :role WHERE chat_id = :chatId")
    suspend fun setSelfRole(chatId: String, role: String)

    @Query("UPDATE groups SET last_message = :lastMessage WHERE chat_id = :chatId")
    suspend fun setLastMessage(chatId: String, lastMessage: Long)

    @Query("UPDATE groups SET has_unread_messages = :hasUnread WHERE chat_id = :chatId")
    suspend fun setHasUnreadMessages(chatId: String, hasUnread: Boolean)

    @Query("UPDATE groups SET draft_message = :draft WHERE chat_id = :chatId")
    suspend fun setDraftMessage(chatId: String, draft: String)

    @Query("UPDATE groups SET connected = :connected WHERE chat_id = :chatId")
    suspend fun setConnected(chatId: String, connected: Boolean)

    @Query("UPDATE groups SET group_number = :groupNumber WHERE chat_id = :chatId")
    suspend fun setGroupNumber(chatId: String, groupNumber: Int)

    @Query("SELECT chat_id FROM groups WHERE group_number = :groupNumber")
    suspend fun findChatIdByGroupNumber(groupNumber: Int): String?

    @Query("DELETE FROM groups WHERE chat_id = :chatId")
    suspend fun deleteByChatId(chatId: String)
}
