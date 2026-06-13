package ltd.evilcorp.domain.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.contacts.repository.IFriendRequestRepository

class FakeFriendRequestRepository : IFriendRequestRepository {
    private val requests = MutableStateFlow<Map<String, FriendRequest>>(emptyMap())

    override suspend fun add(friendRequest: FriendRequest) {
        requests.value = requests.value + (friendRequest.publicKey to friendRequest)
    }

    override suspend fun delete(friendRequest: FriendRequest) {
        requests.value = requests.value - friendRequest.publicKey
    }

    override fun getAll(): Flow<List<FriendRequest>> {
        return requests.map { it.values.toList() }
    }

    override fun get(publicKey: String): Flow<FriendRequest?> {
        return requests.map { it[publicKey] }
    }

    override suspend fun count(): Int {
        return requests.value.size
    }
}
