package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import ltd.evilcorp.core.db.dao.FriendRequestDao
import ltd.evilcorp.core.db.entity.FriendRequestEntity
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.contacts.repository.IFriendRequestRepository
import ltd.evilcorp.core.profile.ProfileManager
import android.content.Context

@Singleton
class FriendRequestRepositoryImpl @Inject internal constructor(
    private val friendRequestDao: FriendRequestDao,
    private val dbProvider: javax.inject.Provider<ltd.evilcorp.core.db.Database>? = null,
    private val context: Context? = null
) : IFriendRequestRepository {
    private val activeDao: FriendRequestDao get() = dbProvider?.get()?.friendRequestDao() ?: friendRequestDao
    private val profileIdFlow = MutableStateFlow(context?.let { ProfileManager.getActiveProfileId(it) } ?: ProfileManager.DEFAULT_PROFILE_ID)

    private fun updateProfileIdIfNeeded() {
        if (context != null) {
            val currentProfileId = ProfileManager.getActiveProfileId(context)
            if (profileIdFlow.value != currentProfileId) {
                profileIdFlow.value = currentProfileId
            }
        }
    }

    override suspend fun add(friendRequest: FriendRequest) {
        updateProfileIdIfNeeded()
        activeDao.save(FriendRequestEntity.fromDomain(friendRequest))
    }

    override suspend fun delete(friendRequest: FriendRequest) {
        updateProfileIdIfNeeded()
        activeDao.delete(FriendRequestEntity.fromDomain(friendRequest))
    }

    override fun getAll(): Flow<List<FriendRequest>> = profileIdFlow
        .flatMapLatest { activeDao.loadAll().map { list -> list.map { it.toDomain() } } }

    override fun get(publicKey: String): Flow<FriendRequest?> = profileIdFlow
        .flatMapLatest { activeDao.load(publicKey).map { it?.toDomain() } }

    override suspend fun count(): Int {
        updateProfileIdIfNeeded()
        return activeDao.count()
    }
}
