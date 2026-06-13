package ltd.evilcorp.core.repository

import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.dao.GroupDao
import ltd.evilcorp.core.db.dao.GroupMessageDao
import ltd.evilcorp.core.db.dao.GroupPeerDao
import ltd.evilcorp.core.db.entity.GroupEntity
import ltd.evilcorp.core.db.entity.GroupMessageEntity
import ltd.evilcorp.core.db.entity.GroupPeerEntity
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.group.repository.IGroupRepository

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val groupDao: GroupDao,
    private val groupMessageDao: GroupMessageDao,
    private val groupPeerDao: GroupPeerDao,
    private val dbProvider: javax.inject.Provider<ltd.evilcorp.core.db.Database>? = null
) : IGroupRepository {
    private val activeGroupDao: GroupDao get() = dbProvider?.get()?.groupDao() ?: groupDao
    private val activeGroupMessageDao: GroupMessageDao get() = dbProvider?.get()?.groupMessageDao() ?: groupMessageDao
    private val activeGroupPeerDao: GroupPeerDao get() = dbProvider?.get()?.groupPeerDao() ?: groupPeerDao

    override fun get(chatId: String): Flow<Group?> = activeGroupDao.load(chatId).map { it?.toDomain() }
    override suspend fun getDirect(chatId: String): Group? = activeGroupDao.loadDirect(chatId)?.toDomain()
    override fun getAll(): Flow<List<Group>> = activeGroupDao.loadAll().map { list -> list.map { it.toDomain() } }
    override suspend fun exists(chatId: String): Boolean = activeGroupDao.exists(chatId) > 0

    override suspend fun add(group: Group) = activeGroupDao.save(GroupEntity.fromDomain(group))
    override suspend fun update(group: Group) = activeGroupDao.update(GroupEntity.fromDomain(group))
    override suspend fun delete(group: Group) = activeGroupDao.delete(GroupEntity.fromDomain(group))
    override suspend fun deleteByChatId(chatId: String) = activeGroupDao.deleteByChatId(chatId)

    override suspend fun setName(chatId: String, name: String) = activeGroupDao.setName(chatId, name)
    override suspend fun setTopic(chatId: String, topic: String) = activeGroupDao.setTopic(chatId, topic)
    override suspend fun setPasswordProtected(chatId: String, protected: Boolean) = activeGroupDao.setPasswordProtected(chatId, protected)
    override suspend fun setPrivacyState(chatId: String, privacyState: GroupPrivacyState) = activeGroupDao.setPrivacyState(chatId, privacyState)
    override suspend fun setPeerCount(chatId: String, peerCount: Int) = activeGroupDao.setPeerCount(chatId, peerCount)
    override suspend fun setSelfPeerId(chatId: String, peerId: Int) = activeGroupDao.setSelfPeerId(chatId, peerId)
    override suspend fun setSelfRole(chatId: String, role: String) = activeGroupDao.setSelfRole(chatId, role)
    override suspend fun setLastMessage(chatId: String, lastMessage: Long) = activeGroupDao.setLastMessage(chatId, lastMessage)
    override suspend fun setHasUnreadMessages(chatId: String, hasUnread: Boolean) = activeGroupDao.setHasUnreadMessages(chatId, hasUnread)
    override suspend fun setDraftMessage(chatId: String, draft: String) = activeGroupDao.setDraftMessage(chatId, draft)
    override suspend fun setConnected(chatId: String, connected: Boolean) = activeGroupDao.setConnected(chatId, connected)
    override suspend fun setGroupNumber(chatId: String, groupNumber: Int) = activeGroupDao.setGroupNumber(chatId, groupNumber)
    override suspend fun findChatIdByGroupNumber(groupNumber: Int): String? = activeGroupDao.findChatIdByGroupNumber(groupNumber)

    override suspend fun addMessage(message: GroupMessage) {
        activeGroupMessageDao.save(GroupMessageEntity.fromDomain(message))
        activeGroupDao.setLastMessage(message.groupChatId, Date().time)
    }

    override fun getMessages(groupChatId: String): Flow<List<GroupMessage>> =
        activeGroupMessageDao.load(groupChatId).map { list -> list.map { it.toDomain() } }

    override suspend fun getPendingMessages(groupChatId: String): List<GroupMessage> =
        activeGroupMessageDao.loadPending(groupChatId).map { it.toDomain() }

    override suspend fun getUnsentMessages(groupChatId: String): List<GroupMessage> =
        activeGroupMessageDao.loadUnsent(groupChatId).map { it.toDomain() }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) = activeGroupMessageDao.setCorrelationId(id, correlationId)
    override suspend fun deleteMessages(groupChatId: String) = activeGroupMessageDao.delete(groupChatId)
    override suspend fun deleteMessage(id: Long) = activeGroupMessageDao.deleteMessage(id)
    override suspend fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long) =
        activeGroupMessageDao.setReceipt(groupChatId, correlationId, timestamp)

    override suspend fun existsByCorrelationId(groupChatId: String, correlationId: Int): Boolean =
        activeGroupMessageDao.existsByCorrelationId(groupChatId, correlationId) > 0

    override suspend fun getMessageIds(groupChatId: String): List<Int> =
        activeGroupMessageDao.getMessageIds(groupChatId)

    override suspend fun getMessagesByIds(groupChatId: String, ids: Set<Int>): List<GroupMessage> =
        activeGroupMessageDao.getMessagesByIds(groupChatId, ids).map { it.toDomain() }

    override suspend fun addPeer(peer: GroupPeer) = activeGroupPeerDao.save(GroupPeerEntity.fromDomain(peer))
    override suspend fun updatePeer(peer: GroupPeer) = activeGroupPeerDao.update(GroupPeerEntity.fromDomain(peer))
    override suspend fun deletePeer(peer: GroupPeer) = activeGroupPeerDao.delete(GroupPeerEntity.fromDomain(peer))
    override suspend fun deletePeerById(groupChatId: String, peerId: Int) = activeGroupPeerDao.deleteByPeerId(groupChatId, peerId)
    override suspend fun deleteAllPeers(groupChatId: String) = activeGroupPeerDao.deleteAllForGroup(groupChatId)

    override fun getPeers(groupChatId: String): Flow<List<GroupPeer>> =
        activeGroupPeerDao.loadAllForGroup(groupChatId).map { list -> list.map { it.toDomain() } }

    override fun getPeer(groupChatId: String, peerId: Int): Flow<GroupPeer?> =
        activeGroupPeerDao.load(groupChatId, peerId).map { it?.toDomain() }

    override suspend fun getPeerNameDirect(groupChatId: String, peerId: Int): String? = activeGroupPeerDao.getPeerNameDirect(groupChatId, peerId)
    override suspend fun peerExistsDirect(groupChatId: String, peerId: Int): Boolean = activeGroupPeerDao.peerExistsDirect(groupChatId, peerId) > 0
    override suspend fun peerExistsByPublicKey(groupChatId: String, publicKey: String): Boolean =
        activeGroupPeerDao.peerExistsByPublicKeyDirect(groupChatId, publicKey) > 0
    override suspend fun deletePeerByPublicKey(groupChatId: String, publicKey: String) = activeGroupPeerDao.deleteByPublicKey(groupChatId, publicKey)
    override suspend fun setPeerName(groupChatId: String, peerId: Int, name: String) = activeGroupPeerDao.setName(groupChatId, peerId, name)
    override suspend fun setPeerRole(groupChatId: String, peerId: Int, role: String) = activeGroupPeerDao.setRole(groupChatId, peerId, role)
    override suspend fun setPeerStatus(groupChatId: String, peerId: Int, status: UserStatus) = activeGroupPeerDao.setStatus(groupChatId, peerId, status)
    override fun peerCount(groupChatId: String): Flow<Int> = activeGroupPeerDao.countForGroup(groupChatId)
    override suspend fun peerCountDirect(groupChatId: String): Int = activeGroupPeerDao.countForGroupDirect(groupChatId)
}
