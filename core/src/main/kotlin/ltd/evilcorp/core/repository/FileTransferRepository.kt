package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.FileTransferDao
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.domain.repository.IFileTransferRepository

@Singleton
class FileTransferRepository @Inject internal constructor(private val dao: FileTransferDao) : IFileTransferRepository {
    override fun add(ft: FileTransfer): Long = dao.save(ft)

    override fun delete(id: Int) = dao.delete(id)

    override fun get(publicKey: String): Flow<List<FileTransfer>> = dao.load(publicKey)

    override fun get(id: Int): Flow<FileTransfer> = dao.load(id)

    override fun setDestination(id: Int, destination: String) = dao.setDestination(id, destination)

    override fun updateProgress(id: Int, progress: Long) = dao.updateProgress(id, progress)

    override fun resetTransientData() = dao.resetTransientData()
}
