package ltd.evilcorp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.entity.UserEntity
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun save(user: UserEntity)

    @Update
    fun update(user: UserEntity)

    @Query("UPDATE users SET name = :name WHERE public_key == :publicKey")
    fun updateName(publicKey: String, name: String)

    @Query("UPDATE users SET status_message = :statusMessage WHERE public_key == :publicKey")
    fun updateStatusMessage(publicKey: String, statusMessage: String)

    @Query("UPDATE users SET connection_status = :connectionStatus WHERE public_key == :publicKey")
    fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus)

    @Query("UPDATE users SET status = :status WHERE public_key == :publicKey")
    fun updateStatus(publicKey: String, status: UserStatus)

    @Query("SELECT COUNT(*) FROM users WHERE public_key = :publicKey")
    fun exists(publicKey: String): Boolean

    @Query("SELECT * FROM users WHERE public_key = :publicKey")
    fun load(publicKey: String): Flow<UserEntity?>
}
