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
import ltd.evilcorp.domain.repository.IGroupRepository

@Singleton
class GroupRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val groupMessageDao: GroupMessageDao,
    private val groupPeerDao: GroupPeerDao,
) : IGroupRepository {
    override fun get(chatId: String): Flow<Group?> = groupDao.load(chatId)
    override fun getDirect(chatId: String): Group? = groupDao.loadDirect(chatId)
    override fun getAll(): Flow<List<Group>> = groupDao.loadAll()
    override fun exists(chatId: String): Boolean = groupDao.exists(chatId) > 0

    override fun add(group: Group) = groupDao.save(group)
    override fun update(group: Group) = groupDao.update(group)
    override fun delete(group: Group) = groupDao.delete(group)
    override fun deleteByChatId(chatId: String) = groupDao.deleteByChatId(chatId)

    override fun setName(chatId: String, name: String) = groupDao.setName(chatId, name)
    override fun setTopic(chatId: String, topic: String) = groupDao.setTopic(chatId, topic)
    override fun setPasswordProtected(chatId: String, protected: Boolean) = groupDao.setPasswordProtected(chatId, protected)
    override fun setPrivacyState(chatId: String, privacyState: GroupPrivacyState) = groupDao.setPrivacyState(chatId, privacyState)
    override fun setPeerCount(chatId: String, peerCount: Int) = groupDao.setPeerCount(chatId, peerCount)
    override fun setSelfPeerId(chatId: String, peerId: Int) = groupDao.setSelfPeerId(chatId, peerId)
    override fun setSelfRole(chatId: String, role: String) = groupDao.setSelfRole(chatId, role)
    override fun setLastMessage(chatId: String, lastMessage: Long) = groupDao.setLastMessage(chatId, lastMessage)
    override fun setHasUnreadMessages(chatId: String, hasUnread: Boolean) = groupDao.setHasUnreadMessages(chatId, hasUnread)
    override fun setDraftMessage(chatId: String, draft: String) = groupDao.setDraftMessage(chatId, draft)
    override fun setConnected(chatId: String, connected: Boolean) = groupDao.setConnected(chatId, connected)
    override fun setGroupNumber(chatId: String, groupNumber: Int) = groupDao.setGroupNumber(chatId, groupNumber)
    override fun findChatIdByGroupNumber(groupNumber: Int): String? = groupDao.findChatIdByGroupNumber(groupNumber)

    override fun addMessage(message: GroupMessage) {
        groupMessageDao.save(message)
        groupDao.setLastMessage(message.groupChatId, Date().time)
    }

    override fun getMessages(groupChatId: String): Flow<List<GroupMessage>> = groupMessageDao.load(groupChatId)
    override fun getPendingMessages(groupChatId: String): List<GroupMessage> = groupMessageDao.loadPending(groupChatId)
    override fun getUnsentMessages(groupChatId: String): List<GroupMessage> = groupMessageDao.loadUnsent(groupChatId)
    override fun setCorrelationId(id: Long, correlationId: Int) = groupMessageDao.setCorrelationId(id, correlationId)
    override fun deleteMessages(groupChatId: String) = groupMessageDao.delete(groupChatId)
    override fun deleteMessage(id: Long) = groupMessageDao.deleteMessage(id)
    override fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long) =
        groupMessageDao.setReceipt(groupChatId, correlationId, timestamp)

    override fun existsByCorrelationId(groupChatId: String, correlationId: Int): Boolean =
        groupMessageDao.existsByCorrelationId(groupChatId, correlationId) > 0

    override fun addPeer(peer: GroupPeer) = groupPeerDao.save(peer)
    override fun updatePeer(peer: GroupPeer) = groupPeerDao.update(peer)
    override fun deletePeer(peer: GroupPeer) = groupPeerDao.delete(peer)
    override fun deletePeerById(groupChatId: String, peerId: Int) = groupPeerDao.deleteByPeerId(groupChatId, peerId)
    override fun deleteAllPeers(groupChatId: String) = groupPeerDao.deleteAllForGroup(groupChatId)

    override fun getPeers(groupChatId: String): Flow<List<GroupPeer>> = groupPeerDao.loadAllForGroup(groupChatId)
    override fun getPeer(groupChatId: String, peerId: Int): Flow<GroupPeer?> = groupPeerDao.load(groupChatId, peerId)
    override fun getPeerNameDirect(groupChatId: String, peerId: Int): String? = groupPeerDao.getPeerNameDirect(groupChatId, peerId)
    override fun peerExistsDirect(groupChatId: String, peerId: Int): Boolean = groupPeerDao.peerExistsDirect(groupChatId, peerId) > 0
    override fun peerExistsByPublicKey(groupChatId: String, publicKey: String): Boolean = groupPeerDao.peerExistsByPublicKeyDirect(groupChatId, publicKey) > 0
    override fun deletePeerByPublicKey(groupChatId: String, publicKey: String) = groupPeerDao.deleteByPublicKey(groupChatId, publicKey)
    override fun setPeerName(groupChatId: String, peerId: Int, name: String) = groupPeerDao.setName(groupChatId, peerId, name)
    override fun setPeerRole(groupChatId: String, peerId: Int, role: String) = groupPeerDao.setRole(groupChatId, peerId, role)
    override fun setPeerStatus(groupChatId: String, peerId: Int, status: UserStatus) = groupPeerDao.setStatus(groupChatId, peerId, status)
    override fun peerCount(groupChatId: String): Flow<Int> = groupPeerDao.countForGroup(groupChatId)
    override suspend fun peerCountDirect(groupChatId: String): Int = groupPeerDao.countForGroupDirect(groupChatId)
}
