package ltd.evilcorp.domain.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.group.repository.IGroupRepository

class FakeGroupRepository : IGroupRepository {
    private val groups = MutableStateFlow<Map<String, Group>>(emptyMap())
    private val messages = MutableStateFlow<List<GroupMessage>>(emptyList())
    private val peers = MutableStateFlow<List<GroupPeer>>(emptyList())

    override fun get(chatId: String): Flow<Group?> {
        return groups.map { it[chatId] }
    }

    override suspend fun getDirect(chatId: String): Group? {
        return groups.value[chatId]
    }

    override fun getAll(): Flow<List<Group>> {
        return groups.map { it.values.toList() }
    }

    override suspend fun exists(chatId: String): Boolean {
        return groups.value.containsKey(chatId)
    }

    override suspend fun add(group: Group) {
        groups.value = groups.value + (group.chatId to group)
    }

    override suspend fun update(group: Group) {
        groups.value = groups.value + (group.chatId to group)
    }

    override suspend fun delete(group: Group) {
        groups.value = groups.value - group.chatId
    }

    override suspend fun deleteByChatId(chatId: String) {
        groups.value = groups.value - chatId
    }

    override suspend fun setName(chatId: String, name: String) {
        updateField(chatId) { it.copy(name = name) }
    }

    override suspend fun setTopic(chatId: String, topic: String) {
        updateField(chatId) { it.copy(topic = topic) }
    }

    override suspend fun setPasswordProtected(chatId: String, protected: Boolean) {
        updateField(chatId) { it.copy(passwordProtected = protected) }
    }

    override suspend fun setPrivacyState(chatId: String, privacyState: GroupPrivacyState) {
        updateField(chatId) { it.copy(privacyState = privacyState) }
    }

    override suspend fun setPeerCount(chatId: String, peerCount: Int) {
        updateField(chatId) { it.copy(peerCount = peerCount) }
    }

    override suspend fun setSelfPeerId(chatId: String, peerId: Int) {
        updateField(chatId) { it.copy(selfPeerId = peerId) }
    }

    override suspend fun setSelfRole(chatId: String, role: String) {
        updateField(chatId) { it.copy(selfRole = role) }
    }

    override suspend fun setLastMessage(chatId: String, lastMessage: Long) {
        updateField(chatId) { it.copy(lastMessage = lastMessage) }
    }

    override suspend fun setHasUnreadMessages(chatId: String, hasUnread: Boolean) {
        updateField(chatId) { it.copy(hasUnreadMessages = hasUnread) }
    }

    override suspend fun setDraftMessage(chatId: String, draft: String) {
        updateField(chatId) { it.copy(draftMessage = draft) }
    }

    override suspend fun setConnected(chatId: String, connected: Boolean) {
        updateField(chatId) { it.copy(connected = connected) }
    }

    override suspend fun setGroupNumber(chatId: String, groupNumber: Int) {
        updateField(chatId) { it.copy(groupNumber = groupNumber) }
    }

    override suspend fun findChatIdByGroupNumber(groupNumber: Int): String? {
        return groups.value.values.find { it.groupNumber == groupNumber }?.chatId
    }

    override suspend fun addMessage(message: GroupMessage) {
        messages.value = messages.value + message
    }

    override fun getMessages(groupChatId: String): Flow<List<GroupMessage>> {
        return messages.map { list -> list.filter { it.groupChatId == groupChatId } }
    }

    override suspend fun getPendingMessages(groupChatId: String): List<GroupMessage> {
        return messages.value.filter { it.groupChatId == groupChatId && it.timestamp == 0L }
    }

    override suspend fun getUnsentMessages(groupChatId: String): List<GroupMessage> {
        return messages.value.filter { it.groupChatId == groupChatId && it.timestamp == 0L }
    }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) {
        messages.value = messages.value.map { msg ->
            if (msg.id == id) msg.copy(correlationId = correlationId) else msg
        }
    }

    override suspend fun deleteMessages(groupChatId: String) {
        messages.value = messages.value.filter { it.groupChatId != groupChatId }
    }

    override suspend fun deleteMessage(id: Long) {
        messages.value = messages.value.filter { it.id != id }
    }

    override suspend fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long) {
        messages.value = messages.value.map { msg ->
            if (msg.groupChatId == groupChatId && msg.correlationId == correlationId) {
                msg.copy(timestamp = timestamp)
            } else {
                msg
            }
        }
    }

    override suspend fun existsByCorrelationId(groupChatId: String, correlationId: Int): Boolean {
        return messages.value.any { it.groupChatId == groupChatId && it.correlationId == correlationId }
    }

    override suspend fun getMessageIds(groupChatId: String): List<Int> {
        return messages.value.filter { it.groupChatId == groupChatId }.map { it.id.toInt() }
    }

    override suspend fun getMessagesByIds(groupChatId: String, ids: Set<Int>): List<GroupMessage> {
        return messages.value.filter { it.groupChatId == groupChatId && ids.contains(it.id.toInt()) }
    }

    override suspend fun addPeer(peer: GroupPeer) {
        peers.value = peers.value + peer
    }

    override suspend fun updatePeer(peer: GroupPeer) {
        peers.value = peers.value.map { p ->
            if (p.groupChatId == peer.groupChatId && p.peerId == peer.peerId) peer else p
        }
    }

    override suspend fun deletePeer(peer: GroupPeer) {
        peers.value = peers.value.filterNot { it.groupChatId == peer.groupChatId && it.peerId == peer.peerId }
    }

    override suspend fun deletePeerById(groupChatId: String, peerId: Int) {
        peers.value = peers.value.filterNot { it.groupChatId == groupChatId && it.peerId == peerId }
    }

    override suspend fun deleteAllPeers(groupChatId: String) {
        peers.value = peers.value.filterNot { it.groupChatId == groupChatId }
    }

    override fun getPeers(groupChatId: String): Flow<List<GroupPeer>> {
        return peers.map { list -> list.filter { it.groupChatId == groupChatId } }
    }

    override fun getPeer(groupChatId: String, peerId: Int): Flow<GroupPeer?> {
        return peers.map { list -> list.find { it.groupChatId == groupChatId && it.peerId == peerId } }
    }

    override suspend fun getPeerNameDirect(groupChatId: String, peerId: Int): String? {
        return peers.value.find { it.groupChatId == groupChatId && it.peerId == peerId }?.name
    }

    override suspend fun peerExistsDirect(groupChatId: String, peerId: Int): Boolean {
        return peers.value.any { it.groupChatId == groupChatId && it.peerId == peerId }
    }

    override suspend fun peerExistsByPublicKey(groupChatId: String, publicKey: String): Boolean {
        return peers.value.any { it.groupChatId == groupChatId && it.publicKey == publicKey }
    }

    override suspend fun deletePeerByPublicKey(groupChatId: String, publicKey: String) {
        peers.value = peers.value.filterNot { it.groupChatId == groupChatId && it.publicKey == publicKey }
    }

    override suspend fun setPeerName(groupChatId: String, peerId: Int, name: String) {
        updatePeerField(groupChatId, peerId) { it.copy(name = name) }
    }

    override suspend fun setPeerRole(groupChatId: String, peerId: Int, role: String) {
        updatePeerField(groupChatId, peerId) { it.copy(role = role) }
    }

    override suspend fun setPeerStatus(groupChatId: String, peerId: Int, status: UserStatus) {
        updatePeerField(groupChatId, peerId) { it.copy(status = status) }
    }

    override fun peerCount(groupChatId: String): Flow<Int> {
        return peers.map { list -> list.count { it.groupChatId == groupChatId } }
    }

    override suspend fun peerCountDirect(groupChatId: String): Int {
        return peers.value.count { it.groupChatId == groupChatId }
    }

    private fun updateField(chatId: String, update: (Group) -> Group) {
        val current = groups.value[chatId]
        if (current != null) {
            groups.value = groups.value + (chatId to update(current))
        }
    }

    private fun updatePeerField(groupChatId: String, peerId: Int, update: (GroupPeer) -> GroupPeer) {
        peers.value = peers.value.map { p ->
            if (p.groupChatId == groupChatId && p.peerId == peerId) update(p) else p
        }
    }
}
