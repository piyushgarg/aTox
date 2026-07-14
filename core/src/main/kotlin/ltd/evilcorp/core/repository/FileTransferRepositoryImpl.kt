package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import ltd.evilcorp.core.db.dao.FileTransferDao
import ltd.evilcorp.core.db.entity.FileTransferEntity
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository
import ltd.evilcorp.core.profile.ProfileManager
import android.content.Context

@Singleton
class FileTransferRepositoryImpl @Inject internal constructor(
    private val dao: FileTransferDao,
    private val dbProvider: javax.inject.Provider<ltd.evilcorp.core.db.Database>? = null,
    private val context: Context? = null
) : IFileTransferRepository {
    private val activeDao: FileTransferDao get() = dbProvider?.get()?.fileTransferDao() ?: dao
    private val profileIdFlow = MutableStateFlow(context?.let { ProfileManager.getActiveProfileId(it) } ?: ProfileManager.DEFAULT_PROFILE_ID)

    private fun updateProfileIdIfNeeded() {
        if (context != null) {
            val currentProfileId = ProfileManager.getActiveProfileId(context)
            if (profileIdFlow.value != currentProfileId) {
                profileIdFlow.value = currentProfileId
            }
        }
    }

    override suspend fun add(ft: FileTransfer): Long {
        updateProfileIdIfNeeded()
        return activeDao.save(FileTransferEntity.fromDomain(ft))
    }

    override suspend fun delete(id: Int) {
        updateProfileIdIfNeeded()
        activeDao.delete(id)
    }

    override fun get(publicKey: String): Flow<List<FileTransfer>> = profileIdFlow
        .flatMapLatest { activeDao.load(publicKey).map { list -> list.map { it.toDomain() } } }

    override fun get(id: Int): Flow<FileTransfer> = profileIdFlow
        .flatMapLatest { activeDao.load(id).map { it.toDomain() } }

    override suspend fun setDestination(id: Int, destination: String) {
        updateProfileIdIfNeeded()
        activeDao.setDestination(id, destination)
    }

    override suspend fun updateProgress(id: Int, progress: Long) {
        updateProfileIdIfNeeded()
        activeDao.updateProgress(id, progress)
    }

    override suspend fun resetTransientData() {
        updateProfileIdIfNeeded()
        activeDao.resetTransientData()
    }
}
