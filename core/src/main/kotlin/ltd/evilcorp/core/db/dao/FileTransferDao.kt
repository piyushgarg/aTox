package ltd.evilcorp.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.entity.FileTransferEntity
import ltd.evilcorp.domain.features.transfer.model.FT_REJECTED

@Dao
interface FileTransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(fileTransfer: FileTransferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(fileTransfers: List<FileTransferEntity>)

    @Query("DELETE FROM file_transfers WHERE id == :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM file_transfers WHERE public_key == :publicKey")
    fun load(publicKey: String): Flow<List<FileTransferEntity>>

    @Query("SELECT * FROM file_transfers WHERE id == :id")
    fun load(id: Int): Flow<FileTransferEntity>

    @Query("SELECT * FROM file_transfers")
    suspend fun loadAll(): List<FileTransferEntity>

    @Query("UPDATE file_transfers SET progress = :progress WHERE id == :id AND progress != :rejected")
    suspend fun updateProgress(id: Int, progress: Long, rejected: Long = FT_REJECTED)

    @Query("UPDATE file_transfers SET destination = :destination WHERE id == :id")
    suspend fun setDestination(id: Int, destination: String)

    @Query("UPDATE file_transfers SET progress = :progress WHERE progress < file_size")
    suspend fun resetTransientData(progress: Long = FT_REJECTED)
}
