package ltd.evilcorp.core.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.entity.FriendRequestEntity

@Dao
interface FriendRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(friendRequest: FriendRequestEntity)

    @Delete
    suspend fun delete(friendRequest: FriendRequestEntity)

    @Query("SELECT * FROM friend_requests")
    fun loadAll(): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE public_key == :publicKey")
    fun load(publicKey: String): Flow<FriendRequestEntity?>

    @Query("SELECT COUNT(public_key) FROM friend_requests")
    suspend fun count(): Int
}
