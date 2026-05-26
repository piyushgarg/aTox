package ltd.evilcorp.domain.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.GroupPeer
import ltd.evilcorp.core.model.GroupPrivacyState
import ltd.evilcorp.core.model.UserStatus

interface IGroupRepository {
    fun get(chatId: String): Flow<Group?>
    fun getDirect(chatId: String): Group?
    fun getAll(): Flow<List<Group>>
    fun exists(chatId: String): Boolean

    fun add(group: Group)
    fun update(group: Group)
    fun delete(group: Group)
    fun deleteByChatId(chatId: String)

    fun setName(chatId: String, name: String)
    fun setTopic(chatId: String, topic: String)
    fun setPasswordProtected(chatId: String, protected: Boolean)
    fun setPrivacyState(chatId: String, privacyState: GroupPrivacyState)
    fun setPeerCount(chatId: String, peerCount: Int)
    fun setSelfPeerId(chatId: String, peerId: Int)
    fun setSelfRole(chatId: String, role: String)
    fun setLastMessage(chatId: String, lastMessage: Long)
    fun setHasUnreadMessages(chatId: String, hasUnread: Boolean)
    fun setDraftMessage(chatId: String, draft: String)
    fun setConnected(chatId: String, connected: Boolean)
    fun setGroupNumber(chatId: String, groupNumber: Int)
    fun findChatIdByGroupNumber(groupNumber: Int): String?

    fun addMessage(message: GroupMessage)
    fun getMessages(groupChatId: String): Flow<List<GroupMessage>>
    fun getPendingMessages(groupChatId: String): List<GroupMessage>
    fun getUnsentMessages(groupChatId: String): List<GroupMessage>
    fun setCorrelationId(id: Long, correlationId: Int)
    fun deleteMessages(groupChatId: String)
    fun deleteMessage(id: Long)
    fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long)
    fun existsByCorrelationId(groupChatId: String, correlationId: Int): Boolean

    fun addPeer(peer: GroupPeer)
    fun updatePeer(peer: GroupPeer)
    fun deletePeer(peer: GroupPeer)
    fun deletePeerById(groupChatId: String, peerId: Int)
    fun deleteAllPeers(groupChatId: String)

    fun getPeers(groupChatId: String): Flow<List<GroupPeer>>
    fun getPeer(groupChatId: String, peerId: Int): Flow<GroupPeer?>
    fun getPeerNameDirect(groupChatId: String, peerId: Int): String?
    fun peerExistsDirect(groupChatId: String, peerId: Int): Boolean
    fun peerExistsByPublicKey(groupChatId: String, publicKey: String): Boolean
    fun deletePeerByPublicKey(groupChatId: String, publicKey: String)
    fun setPeerName(groupChatId: String, peerId: Int, name: String)
    fun setPeerRole(groupChatId: String, peerId: Int, role: String)
    fun setPeerStatus(groupChatId: String, peerId: Int, status: UserStatus)
    fun peerCount(groupChatId: String): Flow<Int>
    suspend fun peerCountDirect(groupChatId: String): Int
}
