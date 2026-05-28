package ltd.evilcorp.domain.features.transfer.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.transfer.model.FileTransfer

interface IFileTransferRepository {
    suspend fun add(ft: FileTransfer): Long
    suspend fun delete(id: Int)
    fun get(publicKey: String): Flow<List<FileTransfer>>
    fun get(id: Int): Flow<FileTransfer>
    suspend fun setDestination(id: Int, destination: String)
    suspend fun updateProgress(id: Int, progress: Long)
    suspend fun resetTransientData()
}
