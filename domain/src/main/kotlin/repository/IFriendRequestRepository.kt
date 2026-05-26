package ltd.evilcorp.domain.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.model.FriendRequest

interface IFriendRequestRepository {
    fun add(friendRequest: FriendRequest)
    fun delete(friendRequest: FriendRequest)
    fun getAll(): Flow<List<FriendRequest>>
    fun get(publicKey: String): Flow<FriendRequest?>
    fun count(): Int
}
