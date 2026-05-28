package ltd.evilcorp.core.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(contact: ContactEntity)

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("SELECT COUNT(*) FROM contacts WHERE public_key = :publicKey")
    suspend fun exists(publicKey: String): Boolean

    @Query("SELECT * FROM contacts WHERE public_key = :publicKey")
    fun load(publicKey: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts")
    fun loadAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts")
    fun loadAllBlocking(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(contacts: List<ContactEntity>)

    @Transaction
    @Query("UPDATE contacts SET connection_status = :status, typing = :typing")
    suspend fun resetTransientData(status: ConnectionStatus = ConnectionStatus.None, typing: Boolean = false)

    @Query("UPDATE contacts SET name = :name WHERE public_key = :publicKey")
    suspend fun setName(publicKey: String, name: String)

    @Query("UPDATE contacts SET status_message = :statusMessage WHERE public_key = :publicKey")
    suspend fun setStatusMessage(publicKey: String, statusMessage: String)

    @Query("UPDATE contacts SET last_message = :lastMessage WHERE public_key = :publicKey")
    suspend fun setLastMessage(publicKey: String, lastMessage: Long)

    @Query("UPDATE contacts SET status = :status WHERE public_key = :publicKey")
    suspend fun setUserStatus(publicKey: String, status: UserStatus)

    @Query("UPDATE contacts SET connection_status = :connectionStatus WHERE public_key = :publicKey")
    suspend fun setConnectionStatus(publicKey: String, connectionStatus: ConnectionStatus)

    @Query("UPDATE contacts SET typing = :typing WHERE public_key = :publicKey")
    suspend fun setTyping(publicKey: String, typing: Boolean)

    @Query("UPDATE contacts SET avatar_uri = :uri WHERE public_key = :publicKey")
    suspend fun setAvatarUri(publicKey: String, uri: String)

    @Query("UPDATE contacts SET has_unread_messages = :anyUnread WHERE public_key = :publicKey")
    suspend fun setHasUnreadMessages(publicKey: String, anyUnread: Boolean)

    @Query("UPDATE contacts SET draft_message = :draft WHERE public_key = :publicKey")
    suspend fun setDraftMessage(publicKey: String, draft: String)

    @Query("UPDATE contacts SET last_online = :lastOnline WHERE public_key = :publicKey")
    suspend fun setLastOnline(publicKey: String, lastOnline: Long)
}
