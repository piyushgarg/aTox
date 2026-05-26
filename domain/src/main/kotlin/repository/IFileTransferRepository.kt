package ltd.evilcorp.domain.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.model.FileTransfer

interface IFileTransferRepository {
    fun add(ft: FileTransfer): Long
    fun delete(id: Int)
    fun get(publicKey: String): Flow<List<FileTransfer>>
    fun get(id: Int): Flow<FileTransfer>
    fun setDestination(id: Int, destination: String)
    fun updateProgress(id: Int, progress: Long)
    fun resetTransientData()
}
