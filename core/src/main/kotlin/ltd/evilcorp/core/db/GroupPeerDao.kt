package ltd.evilcorp.core.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.entity.GroupPeerEntity
import ltd.evilcorp.domain.model.UserStatus

@Dao
interface GroupPeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(peer: GroupPeerEntity)

    @Update
    fun update(peer: GroupPeerEntity)

    @Delete
    fun delete(peer: GroupPeerEntity)

    @Query("SELECT * FROM group_peers WHERE group_chat_id = :groupChatId")
    fun loadAllForGroup(groupChatId: String): Flow<List<GroupPeerEntity>>

    @Query("SELECT * FROM group_peers WHERE group_chat_id = :groupChatId AND peer_id = :peerId")
    fun load(groupChatId: String, peerId: Int): Flow<GroupPeerEntity?>

    @Query("SELECT name FROM group_peers WHERE group_chat_id = :groupChatId AND peer_id = :peerId")
    fun getPeerNameDirect(groupChatId: String, peerId: Int): String?

    @Query("SELECT COUNT(*) FROM group_peers WHERE group_chat_id = :groupChatId AND peer_id = :peerId")
    fun peerExistsDirect(groupChatId: String, peerId: Int): Int

    @Query("SELECT COUNT(*) FROM group_peers WHERE group_chat_id = :groupChatId AND public_key = :publicKey")
    fun peerExistsByPublicKeyDirect(groupChatId: String, publicKey: String): Int

    @Query("SELECT * FROM group_peers WHERE group_chat_id = :groupChatId AND public_key = :publicKey")
    fun loadByPublicKey(groupChatId: String, publicKey: String): Flow<GroupPeerEntity?>

    @Query("DELETE FROM group_peers WHERE group_chat_id = :groupChatId AND public_key = :publicKey")
    fun deleteByPublicKey(groupChatId: String, publicKey: String)

    @Query("UPDATE group_peers SET name = :name WHERE group_chat_id = :groupChatId AND peer_id = :peerId")
    fun setName(groupChatId: String, peerId: Int, name: String)

    @Query("UPDATE group_peers SET role = :role WHERE group_chat_id = :groupChatId AND peer_id = :peerId")
    fun setRole(groupChatId: String, peerId: Int, role: String)

    @Query("UPDATE group_peers SET status = :status WHERE group_chat_id = :groupChatId AND peer_id = :peerId")
    fun setStatus(groupChatId: String, peerId: Int, status: UserStatus)

    @Query("DELETE FROM group_peers WHERE group_chat_id = :groupChatId AND peer_id = :peerId")
    fun deleteByPeerId(groupChatId: String, peerId: Int)

    @Query("DELETE FROM group_peers WHERE group_chat_id = :groupChatId")
    fun deleteAllForGroup(groupChatId: String)

    @Query("SELECT COUNT(*) FROM group_peers WHERE group_chat_id = :groupChatId")
    fun countForGroup(groupChatId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM group_peers WHERE group_chat_id = :groupChatId")
    suspend fun countForGroupDirect(groupChatId: String): Int
}
