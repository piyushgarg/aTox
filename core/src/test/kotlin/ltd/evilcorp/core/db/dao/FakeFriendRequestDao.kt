package ltd.evilcorp.core.db.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.entity.FriendRequestEntity

class FakeFriendRequestDao : FriendRequestDao {
    private val requests = MutableStateFlow<Map<String, FriendRequestEntity>>(emptyMap())

    override suspend fun save(friendRequest: FriendRequestEntity) {
        requests.value = requests.value + (friendRequest.publicKey to friendRequest)
    }

    override suspend fun delete(friendRequest: FriendRequestEntity) {
        requests.value = requests.value - friendRequest.publicKey
    }

    override fun loadAll(): Flow<List<FriendRequestEntity>> {
        return requests.map { it.values.toList() }
    }

    override fun load(publicKey: String): Flow<FriendRequestEntity?> {
        return requests.map { it[publicKey] }
    }

    override suspend fun count(): Int {
        return requests.value.size
    }
}
