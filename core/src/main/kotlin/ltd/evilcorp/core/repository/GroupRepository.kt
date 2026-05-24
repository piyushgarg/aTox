package ltd.evilcorp.core.repository

import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.GroupDao
import ltd.evilcorp.core.db.GroupMessageDao
import ltd.evilcorp.core.db.GroupPeerDao
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.GroupPeer
import ltd.evilcorp.core.model.GroupPrivacyState
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.model.UserStatus

@Singleton
class GroupRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val groupMessageDao: GroupMessageDao,
    private val groupPeerDao: GroupPeerDao,
) {
    fun get(chatId: String): Flow<Group?> = groupDao.load(chatId)
    fun getDirect(chatId: String): Group? = groupDao.loadDirect(chatId)
    fun getAll(): Flow<List<Group>> = groupDao.loadAll()
    fun exists(chatId: String): Boolean = groupDao.exists(chatId) > 0

    fun add(group: Group) = groupDao.save(group)
    fun update(group: Group) = groupDao.update(group)
    fun delete(group: Group) = groupDao.delete(group)
    fun deleteByChatId(chatId: String) = groupDao.deleteByChatId(chatId)

    fun setName(chatId: String, name: String) = groupDao.setName(chatId, name)
    fun setTopic(chatId: String, topic: String) = groupDao.setTopic(chatId, topic)
    fun setPasswordProtected(chatId: String, protected: Boolean) = groupDao.setPasswordProtected(chatId, protected)
    fun setPrivacyState(chatId: String, privacyState: GroupPrivacyState) = groupDao.setPrivacyState(chatId, privacyState)
    fun setPeerCount(chatId: String, peerCount: Int) = groupDao.setPeerCount(chatId, peerCount)
    fun setSelfPeerId(chatId: String, peerId: Int) = groupDao.setSelfPeerId(chatId, peerId)
    fun setSelfRole(chatId: String, role: String) = groupDao.setSelfRole(chatId, role)
    fun setLastMessage(chatId: String, lastMessage: Long) = groupDao.setLastMessage(chatId, lastMessage)
    fun setHasUnreadMessages(chatId: String, hasUnread: Boolean) = groupDao.setHasUnreadMessages(chatId, hasUnread)
    fun setDraftMessage(chatId: String, draft: String) = groupDao.setDraftMessage(chatId, draft)
    fun setConnected(chatId: String, connected: Boolean) = groupDao.setConnected(chatId, connected)
    fun setGroupNumber(chatId: String, groupNumber: Int) = groupDao.setGroupNumber(chatId, groupNumber)
    fun findChatIdByGroupNumber(groupNumber: Int): String? = groupDao.findChatIdByGroupNumber(groupNumber)

    fun addMessage(message: GroupMessage) {
        groupMessageDao.save(message)
        groupDao.setLastMessage(message.groupChatId, Date().time)
    }

    fun getMessages(groupChatId: String): Flow<List<GroupMessage>> = groupMessageDao.load(groupChatId)
    fun getPendingMessages(groupChatId: String): List<GroupMessage> = groupMessageDao.loadPending(groupChatId)
    fun getUnsentMessages(groupChatId: String): List<GroupMessage> = groupMessageDao.loadUnsent(groupChatId)
    fun setCorrelationId(id: Long, correlationId: Int) = groupMessageDao.setCorrelationId(id, correlationId)
    fun deleteMessages(groupChatId: String) = groupMessageDao.delete(groupChatId)
    fun deleteMessage(id: Long) = groupMessageDao.deleteMessage(id)
    fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long) =
        groupMessageDao.setReceipt(groupChatId, correlationId, timestamp)

    fun existsByCorrelationId(groupChatId: String, correlationId: Int): Boolean =
        groupMessageDao.existsByCorrelationId(groupChatId, correlationId) > 0

    fun addPeer(peer: GroupPeer) = groupPeerDao.save(peer)
    fun updatePeer(peer: GroupPeer) = groupPeerDao.update(peer)
    fun deletePeer(peer: GroupPeer) = groupPeerDao.delete(peer)
    fun deletePeerById(groupChatId: String, peerId: Int) = groupPeerDao.deleteByPeerId(groupChatId, peerId)
    fun deleteAllPeers(groupChatId: String) = groupPeerDao.deleteAllForGroup(groupChatId)

    fun getPeers(groupChatId: String): Flow<List<GroupPeer>> = groupPeerDao.loadAllForGroup(groupChatId)
    fun getPeer(groupChatId: String, peerId: Int): Flow<GroupPeer?> = groupPeerDao.load(groupChatId, peerId)
    fun getPeerNameDirect(groupChatId: String, peerId: Int): String? = groupPeerDao.getPeerNameDirect(groupChatId, peerId)
    fun peerExistsDirect(groupChatId: String, peerId: Int): Boolean = groupPeerDao.peerExistsDirect(groupChatId, peerId) > 0
    fun peerExistsByPublicKey(groupChatId: String, publicKey: String): Boolean = groupPeerDao.peerExistsByPublicKeyDirect(groupChatId, publicKey) > 0
    fun deletePeerByPublicKey(groupChatId: String, publicKey: String) = groupPeerDao.deleteByPublicKey(groupChatId, publicKey)
    fun setPeerName(groupChatId: String, peerId: Int, name: String) = groupPeerDao.setName(groupChatId, peerId, name)
    fun setPeerRole(groupChatId: String, peerId: Int, role: String) = groupPeerDao.setRole(groupChatId, peerId, role)
    fun setPeerStatus(groupChatId: String, peerId: Int, status: UserStatus) = groupPeerDao.setStatus(groupChatId, peerId, status)
    fun peerCount(groupChatId: String): Flow<Int> = groupPeerDao.countForGroup(groupChatId)
    suspend fun peerCountDirect(groupChatId: String): Int = groupPeerDao.countForGroupDirect(groupChatId)
}
