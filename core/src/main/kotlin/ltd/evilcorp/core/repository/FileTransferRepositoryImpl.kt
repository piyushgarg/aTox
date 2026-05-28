package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.dao.FileTransferDao
import ltd.evilcorp.core.db.entity.FileTransferEntity
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository

@Singleton
class FileTransferRepositoryImpl @Inject internal constructor(private val dao: FileTransferDao) : IFileTransferRepository {
    override suspend fun add(ft: FileTransfer): Long = dao.save(FileTransferEntity.fromDomain(ft))

    override suspend fun delete(id: Int) = dao.delete(id)

    override fun get(publicKey: String): Flow<List<FileTransfer>> =
        dao.load(publicKey).map { list -> list.map { it.toDomain() } }

    override fun get(id: Int): Flow<FileTransfer> = dao.load(id).map { it.toDomain() }

    override suspend fun setDestination(id: Int, destination: String) = dao.setDestination(id, destination)

    override suspend fun updateProgress(id: Int, progress: Long) = dao.updateProgress(id, progress)

    override suspend fun resetTransientData() = dao.resetTransientData()
}
