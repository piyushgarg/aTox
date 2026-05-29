package ltd.evilcorp.domain.features.group.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.contacts.model.UserStatus

@Suppress("ComplexInterface")
interface IGroupRepository {
    fun get(chatId: String): Flow<Group?>
    suspend fun getDirect(chatId: String): Group?
    fun getAll(): Flow<List<Group>>
    suspend fun exists(chatId: String): Boolean

    suspend fun add(group: Group)
    suspend fun update(group: Group)
    suspend fun delete(group: Group)
    suspend fun deleteByChatId(chatId: String)

    suspend fun setName(chatId: String, name: String)
    suspend fun setTopic(chatId: String, topic: String)
    suspend fun setPasswordProtected(chatId: String, protected: Boolean)
    suspend fun setPrivacyState(chatId: String, privacyState: GroupPrivacyState)
    suspend fun setPeerCount(chatId: String, peerCount: Int)
    suspend fun setSelfPeerId(chatId: String, peerId: Int)
    suspend fun setSelfRole(chatId: String, role: String)
    suspend fun setLastMessage(chatId: String, lastMessage: Long)
    suspend fun setHasUnreadMessages(chatId: String, hasUnread: Boolean)
    suspend fun setDraftMessage(chatId: String, draft: String)
    suspend fun setConnected(chatId: String, connected: Boolean)
    suspend fun setGroupNumber(chatId: String, groupNumber: Int)
    suspend fun findChatIdByGroupNumber(groupNumber: Int): String?

    suspend fun addMessage(message: GroupMessage)
    fun getMessages(groupChatId: String): Flow<List<GroupMessage>>
    suspend fun getPendingMessages(groupChatId: String): List<GroupMessage>
    suspend fun getUnsentMessages(groupChatId: String): List<GroupMessage>
    suspend fun setCorrelationId(id: Long, correlationId: Int)
    suspend fun deleteMessages(groupChatId: String)
    suspend fun deleteMessage(id: Long)
    suspend fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long)
    suspend fun existsByCorrelationId(groupChatId: String, correlationId: Int): Boolean
    suspend fun getMessageIds(groupChatId: String): List<Int>
    suspend fun getMessagesByIds(groupChatId: String, ids: Set<Int>): List<GroupMessage>

    suspend fun addPeer(peer: GroupPeer)
    suspend fun updatePeer(peer: GroupPeer)
    suspend fun deletePeer(peer: GroupPeer)
    suspend fun deletePeerById(groupChatId: String, peerId: Int)
    suspend fun deleteAllPeers(groupChatId: String)

    fun getPeers(groupChatId: String): Flow<List<GroupPeer>>
    fun getPeer(groupChatId: String, peerId: Int): Flow<GroupPeer?>
    suspend fun getPeerNameDirect(groupChatId: String, peerId: Int): String?
    suspend fun peerExistsDirect(groupChatId: String, peerId: Int): Boolean
    suspend fun peerExistsByPublicKey(groupChatId: String, publicKey: String): Boolean
    suspend fun deletePeerByPublicKey(groupChatId: String, publicKey: String)
    suspend fun setPeerName(groupChatId: String, peerId: Int, name: String)
    suspend fun setPeerRole(groupChatId: String, peerId: Int, role: String)
    suspend fun setPeerStatus(groupChatId: String, peerId: Int, status: UserStatus)
    fun peerCount(groupChatId: String): Flow<Int>
    suspend fun peerCountDirect(groupChatId: String): Int
}
