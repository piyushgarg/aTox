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
class FileTransferRepositoryImpl @Inject internal constructor(
    private val dao: FileTransferDao,
    private val dbProvider: javax.inject.Provider<ltd.evilcorp.core.db.Database>? = null
) : IFileTransferRepository {
    private val activeDao: FileTransferDao get() = dbProvider?.get()?.fileTransferDao() ?: dao

    override suspend fun add(ft: FileTransfer): Long = activeDao.save(FileTransferEntity.fromDomain(ft))

    override suspend fun delete(id: Int) = activeDao.delete(id)

    override fun get(publicKey: String): Flow<List<FileTransfer>> =
        activeDao.load(publicKey).map { list -> list.map { it.toDomain() } }

    override fun get(id: Int): Flow<FileTransfer> = activeDao.load(id).map { it.toDomain() }

    override suspend fun setDestination(id: Int, destination: String) = activeDao.setDestination(id, destination)

    override suspend fun updateProgress(id: Int, progress: Long) = activeDao.updateProgress(id, progress)

    override suspend fun resetTransientData() = activeDao.resetTransientData()
}
