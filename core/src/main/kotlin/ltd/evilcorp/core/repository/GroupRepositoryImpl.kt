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
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.group.repository.IGroupRepository

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val groupDao: GroupDao,
    private val groupMessageDao: GroupMessageDao,
    private val groupPeerDao: GroupPeerDao,
) : IGroupRepository {
    override fun get(chatId: String): Flow<Group?> = groupDao.load(chatId).map { it?.toDomain() }
    override suspend fun getDirect(chatId: String): Group? = groupDao.loadDirect(chatId)?.toDomain()
    override fun getAll(): Flow<List<Group>> = groupDao.loadAll().map { list -> list.map { it.toDomain() } }
    override suspend fun exists(chatId: String): Boolean = groupDao.exists(chatId) > 0

    override suspend fun add(group: Group) = groupDao.save(GroupEntity.fromDomain(group))
    override suspend fun update(group: Group) = groupDao.update(GroupEntity.fromDomain(group))
    override suspend fun delete(group: Group) = groupDao.delete(GroupEntity.fromDomain(group))
    override suspend fun deleteByChatId(chatId: String) = groupDao.deleteByChatId(chatId)

    override suspend fun setName(chatId: String, name: String) = groupDao.setName(chatId, name)
    override suspend fun setTopic(chatId: String, topic: String) = groupDao.setTopic(chatId, topic)
    override suspend fun setPasswordProtected(chatId: String, protected: Boolean) = groupDao.setPasswordProtected(chatId, protected)
    override suspend fun setPrivacyState(chatId: String, privacyState: GroupPrivacyState) = groupDao.setPrivacyState(chatId, privacyState)
    override suspend fun setPeerCount(chatId: String, peerCount: Int) = groupDao.setPeerCount(chatId, peerCount)
    override suspend fun setSelfPeerId(chatId: String, peerId: Int) = groupDao.setSelfPeerId(chatId, peerId)
    override suspend fun setSelfRole(chatId: String, role: String) = groupDao.setSelfRole(chatId, role)
    override suspend fun setLastMessage(chatId: String, lastMessage: Long) = groupDao.setLastMessage(chatId, lastMessage)
    override suspend fun setHasUnreadMessages(chatId: String, hasUnread: Boolean) = groupDao.setHasUnreadMessages(chatId, hasUnread)
    override suspend fun setDraftMessage(chatId: String, draft: String) = groupDao.setDraftMessage(chatId, draft)
    override suspend fun setConnected(chatId: String, connected: Boolean) = groupDao.setConnected(chatId, connected)
    override suspend fun setGroupNumber(chatId: String, groupNumber: Int) = groupDao.setGroupNumber(chatId, groupNumber)
    override suspend fun findChatIdByGroupNumber(groupNumber: Int): String? = groupDao.findChatIdByGroupNumber(groupNumber)

    override suspend fun addMessage(message: GroupMessage) {
        groupMessageDao.save(GroupMessageEntity.fromDomain(message))
        groupDao.setLastMessage(message.groupChatId, Date().time)
    }

    override fun getMessages(groupChatId: String): Flow<List<GroupMessage>> =
        groupMessageDao.load(groupChatId).map { list -> list.map { it.toDomain() } }

    override suspend fun getPendingMessages(groupChatId: String): List<GroupMessage> =
        groupMessageDao.loadPending(groupChatId).map { it.toDomain() }

    override suspend fun getUnsentMessages(groupChatId: String): List<GroupMessage> =
        groupMessageDao.loadUnsent(groupChatId).map { it.toDomain() }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) = groupMessageDao.setCorrelationId(id, correlationId)
    override suspend fun deleteMessages(groupChatId: String) = groupMessageDao.delete(groupChatId)
    override suspend fun deleteMessage(id: Long) = groupMessageDao.deleteMessage(id)
    override suspend fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long) =
        groupMessageDao.setReceipt(groupChatId, correlationId, timestamp)

    override suspend fun existsByCorrelationId(groupChatId: String, correlationId: Int): Boolean =
        groupMessageDao.existsByCorrelationId(groupChatId, correlationId) > 0

    override suspend fun getMessageIds(groupChatId: String): List<Int> =
        groupMessageDao.getMessageIds(groupChatId)

    override suspend fun getMessagesByIds(groupChatId: String, ids: Set<Int>): List<GroupMessage> =
        groupMessageDao.getMessagesByIds(groupChatId, ids).map { it.toDomain() }

    override suspend fun addPeer(peer: GroupPeer) = groupPeerDao.save(GroupPeerEntity.fromDomain(peer))
    override suspend fun updatePeer(peer: GroupPeer) = groupPeerDao.update(GroupPeerEntity.fromDomain(peer))
    override suspend fun deletePeer(peer: GroupPeer) = groupPeerDao.delete(GroupPeerEntity.fromDomain(peer))
    override suspend fun deletePeerById(groupChatId: String, peerId: Int) = groupPeerDao.deleteByPeerId(groupChatId, peerId)
    override suspend fun deleteAllPeers(groupChatId: String) = groupPeerDao.deleteAllForGroup(groupChatId)

    override fun getPeers(groupChatId: String): Flow<List<GroupPeer>> =
        groupPeerDao.loadAllForGroup(groupChatId).map { list -> list.map { it.toDomain() } }

    override fun getPeer(groupChatId: String, peerId: Int): Flow<GroupPeer?> =
        groupPeerDao.load(groupChatId, peerId).map { it?.toDomain() }

    override suspend fun getPeerNameDirect(groupChatId: String, peerId: Int): String? = groupPeerDao.getPeerNameDirect(groupChatId, peerId)
    override suspend fun peerExistsDirect(groupChatId: String, peerId: Int): Boolean = groupPeerDao.peerExistsDirect(groupChatId, peerId) > 0
    override suspend fun peerExistsByPublicKey(groupChatId: String, publicKey: String): Boolean =
        groupPeerDao.peerExistsByPublicKeyDirect(groupChatId, publicKey) > 0
    override suspend fun deletePeerByPublicKey(groupChatId: String, publicKey: String) = groupPeerDao.deleteByPublicKey(groupChatId, publicKey)
    override suspend fun setPeerName(groupChatId: String, peerId: Int, name: String) = groupPeerDao.setName(groupChatId, peerId, name)
    override suspend fun setPeerRole(groupChatId: String, peerId: Int, role: String) = groupPeerDao.setRole(groupChatId, peerId, role)
    override suspend fun setPeerStatus(groupChatId: String, peerId: Int, status: UserStatus) = groupPeerDao.setStatus(groupChatId, peerId, status)
    override fun peerCount(groupChatId: String): Flow<Int> = groupPeerDao.countForGroup(groupChatId)
    override suspend fun peerCountDirect(groupChatId: String): Int = groupPeerDao.countForGroupDirect(groupChatId)
}
