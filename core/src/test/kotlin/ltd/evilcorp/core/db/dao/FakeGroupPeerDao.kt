package ltd.evilcorp.core.db.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.entity.GroupPeerEntity
import ltd.evilcorp.domain.features.contacts.model.UserStatus

class FakeGroupPeerDao : GroupPeerDao {
    private val peers = MutableStateFlow<List<GroupPeerEntity>>(emptyList())

    override suspend fun save(peer: GroupPeerEntity) {
        peers.value = peers.value.filter { !(it.groupChatId == peer.groupChatId && it.peerId == peer.peerId) } + peer
    }

    override suspend fun update(peer: GroupPeerEntity) {
        peers.value = peers.value.filter { !(it.groupChatId == peer.groupChatId && it.peerId == peer.peerId) } + peer
    }

    override suspend fun delete(peer: GroupPeerEntity) {
        peers.value = peers.value.filter { !(it.groupChatId == peer.groupChatId && it.peerId == peer.peerId) }
    }

    override fun loadAllForGroup(groupChatId: String): Flow<List<GroupPeerEntity>> {
        return peers.map { list -> list.filter { it.groupChatId == groupChatId } }
    }

    override fun load(groupChatId: String, peerId: Int): Flow<GroupPeerEntity?> {
        return peers.map { list -> list.find { it.groupChatId == groupChatId && it.peerId == peerId } }
    }

    override fun getPeerNameDirect(groupChatId: String, peerId: Int): String? {
        return peers.value.find { it.groupChatId == groupChatId && it.peerId == peerId }?.name
    }

    override fun peerExistsDirect(groupChatId: String, peerId: Int): Int {
        return if (peers.value.any { it.groupChatId == groupChatId && it.peerId == peerId }) 1 else 0
    }

    override fun peerExistsByPublicKeyDirect(groupChatId: String, publicKey: String): Int {
        return if (peers.value.any { it.groupChatId == groupChatId && it.publicKey == publicKey }) 1 else 0
    }

    override fun loadByPublicKey(groupChatId: String, publicKey: String): Flow<GroupPeerEntity?> {
        return peers.map { list -> list.find { it.groupChatId == groupChatId && it.publicKey == publicKey } }
    }

    override suspend fun deleteByPublicKey(groupChatId: String, publicKey: String) {
        peers.value = peers.value.filter { !(it.groupChatId == groupChatId && it.publicKey == publicKey) }
    }

    override suspend fun setName(groupChatId: String, peerId: Int, name: String) {
        peers.value = peers.value.map { peer ->
            if (peer.groupChatId == groupChatId && peer.peerId == peerId) {
                peer.copy(name = name)
            } else {
                peer
            }
        }
    }

    override suspend fun setRole(groupChatId: String, peerId: Int, role: String) {
        peers.value = peers.value.map { peer ->
            if (peer.groupChatId == groupChatId && peer.peerId == peerId) {
                peer.copy(role = role)
            } else {
                peer
            }
        }
    }

    override suspend fun setStatus(groupChatId: String, peerId: Int, status: UserStatus) {
        peers.value = peers.value.map { peer ->
            if (peer.groupChatId == groupChatId && peer.peerId == peerId) {
                peer.copy(status = status)
            } else {
                peer
            }
        }
    }

    override suspend fun deleteByPeerId(groupChatId: String, peerId: Int) {
        peers.value = peers.value.filter { !(it.groupChatId == groupChatId && it.peerId == peerId) }
    }

    override suspend fun deleteAllForGroup(groupChatId: String) {
        peers.value = peers.value.filter { it.groupChatId != groupChatId }
    }

    override fun countForGroup(groupChatId: String): Flow<Int> {
        return peers.map { list -> list.count { it.groupChatId == groupChatId } }
    }

    override suspend fun countForGroupDirect(groupChatId: String): Int {
        return peers.value.count { it.groupChatId == groupChatId }
    }
}
