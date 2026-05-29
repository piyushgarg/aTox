package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.dao.FriendRequestDao
import ltd.evilcorp.core.db.entity.FriendRequestEntity
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.contacts.repository.IFriendRequestRepository

@Singleton
class FriendRequestRepositoryImpl @Inject internal constructor(private val friendRequestDao: FriendRequestDao) : IFriendRequestRepository {
    override suspend fun add(friendRequest: FriendRequest) = friendRequestDao.save(FriendRequestEntity.fromDomain(friendRequest))

    override suspend fun delete(friendRequest: FriendRequest) = friendRequestDao.delete(FriendRequestEntity.fromDomain(friendRequest))

    override fun getAll(): Flow<List<FriendRequest>> =
        friendRequestDao.loadAll().map { list -> list.map { it.toDomain() } }

    override fun get(publicKey: String): Flow<FriendRequest?> =
        friendRequestDao.load(publicKey).map { it?.toDomain() }

    override suspend fun count(): Int = friendRequestDao.count()
}
