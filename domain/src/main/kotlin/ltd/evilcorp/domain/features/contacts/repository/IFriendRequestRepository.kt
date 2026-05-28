package ltd.evilcorp.domain.features.contacts.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.contacts.model.FriendRequest

interface IFriendRequestRepository {
    fun add(friendRequest: FriendRequest)
    fun delete(friendRequest: FriendRequest)
    fun getAll(): Flow<List<FriendRequest>>
    fun get(publicKey: String): Flow<FriendRequest?>
    fun count(): Int
}
