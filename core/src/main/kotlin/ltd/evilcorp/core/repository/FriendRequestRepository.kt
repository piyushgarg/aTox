package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.FriendRequestDao
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.domain.repository.IFriendRequestRepository

@Singleton
class FriendRequestRepository @Inject internal constructor(private val friendRequestDao: FriendRequestDao) : IFriendRequestRepository {
    override fun add(friendRequest: FriendRequest) = friendRequestDao.save(friendRequest)

    override fun delete(friendRequest: FriendRequest) = friendRequestDao.delete(friendRequest)

    override fun getAll(): Flow<List<FriendRequest>> = friendRequestDao.loadAll()

    override fun get(publicKey: String): Flow<FriendRequest?> = friendRequestDao.load(publicKey)

    override fun count(): Int = friendRequestDao.count()
}
