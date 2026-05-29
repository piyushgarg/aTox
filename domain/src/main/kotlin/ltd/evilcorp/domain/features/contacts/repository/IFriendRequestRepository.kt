package ltd.evilcorp.domain.features.contacts.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.contacts.model.FriendRequest

interface IFriendRequestRepository {
    suspend fun add(friendRequest: FriendRequest)
    suspend fun delete(friendRequest: FriendRequest)
    fun getAll(): Flow<List<FriendRequest>>
    fun get(publicKey: String): Flow<FriendRequest?>
    suspend fun count(): Int
}
