package ltd.evilcorp.core.repository

import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
import ltd.evilcorp.core.profile.ProfileManager
import android.content.Context

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val groupDao: GroupDao,
    private val groupMessageDao: GroupMessageDao,
    private val groupPeerDao: GroupPeerDao,
    private val dbProvider: javax.inject.Provider<ltd.evilcorp.core.db.Database>? = null,
    private val context: Context? = null
) : IGroupRepository {
    private val activeGroupDao: GroupDao get() = dbProvider?.get()?.groupDao() ?: groupDao
    private val activeGroupMessageDao: GroupMessageDao get() = dbProvider?.get()?.groupMessageDao() ?: groupMessageDao
    private val activeGroupPeerDao: GroupPeerDao get() = dbProvider?.get()?.groupPeerDao() ?: groupPeerDao
    private val profileIdFlow = MutableStateFlow(context?.let { ProfileManager.getActiveProfileId(it) } ?: ProfileManager.DEFAULT_PROFILE_ID)

    private fun updateProfileIdIfNeeded() {
        if (context != null) {
            val currentProfileId = ProfileManager.getActiveProfileId(context)
            if (profileIdFlow.value != currentProfileId) {
                profileIdFlow.value = currentProfileId
            }
        }
    }

    override fun get(chatId: String): Flow<Group?> = profileIdFlow
        .flatMapLatest { activeGroupDao.load(chatId).map { it?.toDomain() } }

    override suspend fun getDirect(chatId: String): Group? {
        updateProfileIdIfNeeded()
        return activeGroupDao.loadDirect(chatId)?.toDomain()
    }

    override fun getAll(): Flow<List<Group>> = profileIdFlow
        .flatMapLatest { activeGroupDao.loadAll().map { list -> list.map { it.toDomain() } } }
    override suspend fun exists(chatId: String): Boolean {
        updateProfileIdIfNeeded()
        return activeGroupDao.exists(chatId) > 0
    }

    override suspend fun add(group: Group) {
        updateProfileIdIfNeeded()
        activeGroupDao.save(GroupEntity.fromDomain(group))
    }

    override suspend fun update(group: Group) {
        updateProfileIdIfNeeded()
        activeGroupDao.update(GroupEntity.fromDomain(group))
    }

    override suspend fun delete(group: Group) {
        updateProfileIdIfNeeded()
        activeGroupDao.delete(GroupEntity.fromDomain(group))
    }

    override suspend fun deleteByChatId(chatId: String) {
        updateProfileIdIfNeeded()
        activeGroupDao.deleteByChatId(chatId)
    }

    override suspend fun setName(chatId: String, name: String) {
        updateProfileIdIfNeeded()
        activeGroupDao.setName(chatId, name)
    }

    override suspend fun setTopic(chatId: String, topic: String) {
        updateProfileIdIfNeeded()
        activeGroupDao.setTopic(chatId, topic)
    }

    override suspend fun setPasswordProtected(chatId: String, protected: Boolean) {
        updateProfileIdIfNeeded()
        activeGroupDao.setPasswordProtected(chatId, protected)
    }

    override suspend fun setPrivacyState(chatId: String, privacyState: GroupPrivacyState) {
        updateProfileIdIfNeeded()
        activeGroupDao.setPrivacyState(chatId, privacyState)
    }

    override suspend fun setPeerCount(chatId: String, peerCount: Int) {
        updateProfileIdIfNeeded()
        activeGroupDao.setPeerCount(chatId, peerCount)
    }

    override suspend fun setSelfPeerId(chatId: String, peerId: Int) {
        updateProfileIdIfNeeded()
        activeGroupDao.setSelfPeerId(chatId, peerId)
    }

    override suspend fun setSelfRole(chatId: String, role: String) {
        updateProfileIdIfNeeded()
        activeGroupDao.setSelfRole(chatId, role)
    }

    override suspend fun setLastMessage(chatId: String, lastMessage: Long) {
        updateProfileIdIfNeeded()
        activeGroupDao.setLastMessage(chatId, lastMessage)
    }

    override suspend fun setHasUnreadMessages(chatId: String, hasUnread: Boolean) {
        updateProfileIdIfNeeded()
        activeGroupDao.setHasUnreadMessages(chatId, hasUnread)
    }

    override suspend fun setDraftMessage(chatId: String, draft: String) {
        updateProfileIdIfNeeded()
        activeGroupDao.setDraftMessage(chatId, draft)
    }

    override suspend fun setConnected(chatId: String, connected: Boolean) {
        updateProfileIdIfNeeded()
        activeGroupDao.setConnected(chatId, connected)
    }

    override suspend fun resetTransientData() {
        updateProfileIdIfNeeded()
        activeGroupDao.resetConnectionStatuses()
    }

    override suspend fun setGroupNumber(chatId: String, groupNumber: Int) {
        updateProfileIdIfNeeded()
        activeGroupDao.setGroupNumber(chatId, groupNumber)
    }

    override suspend fun findChatIdByGroupNumber(groupNumber: Int): String? {
        updateProfileIdIfNeeded()
        return activeGroupDao.findChatIdByGroupNumber(groupNumber)
    }

    override suspend fun addMessage(message: GroupMessage) {
        updateProfileIdIfNeeded()
        activeGroupMessageDao.save(GroupMessageEntity.fromDomain(message))
        activeGroupDao.setLastMessage(message.groupChatId, Date().time)
    }

    override fun getMessages(groupChatId: String): Flow<List<GroupMessage>> = profileIdFlow
        .flatMapLatest { activeGroupMessageDao.load(groupChatId).map { list -> list.map { it.toDomain() } } }

    override suspend fun getPendingMessages(groupChatId: String): List<GroupMessage> {
        updateProfileIdIfNeeded()
        return activeGroupMessageDao.loadPending(groupChatId).map { it.toDomain() }
    }

    override suspend fun getUnsentMessages(groupChatId: String): List<GroupMessage> {
        updateProfileIdIfNeeded()
        return activeGroupMessageDao.loadUnsent(groupChatId).map { it.toDomain() }
    }

    override suspend fun setCorrelationId(id: Long, correlationId: Int) {
        updateProfileIdIfNeeded()
        activeGroupMessageDao.setCorrelationId(id, correlationId)
    }

    override suspend fun deleteMessages(groupChatId: String) {
        updateProfileIdIfNeeded()
        activeGroupMessageDao.delete(groupChatId)
    }

    override suspend fun deleteMessage(id: Long) {
        updateProfileIdIfNeeded()
        activeGroupMessageDao.deleteMessage(id)
    }

    override suspend fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long) {
        updateProfileIdIfNeeded()
        activeGroupMessageDao.setReceipt(groupChatId, correlationId, timestamp)
    }

    override suspend fun existsByCorrelationId(groupChatId: String, correlationId: Int): Boolean {
        updateProfileIdIfNeeded()
        return activeGroupMessageDao.existsByCorrelationId(groupChatId, correlationId) > 0
    }

    override suspend fun getMessageIds(groupChatId: String): List<Int> {
        updateProfileIdIfNeeded()
        return activeGroupMessageDao.getMessageIds(groupChatId)
    }

    override suspend fun getMessagesByIds(groupChatId: String, ids: Set<Int>): List<GroupMessage> {
        updateProfileIdIfNeeded()
        return activeGroupMessageDao.getMessagesByIds(groupChatId, ids).map { it.toDomain() }
    }

    override suspend fun addPeer(peer: GroupPeer) {
        updateProfileIdIfNeeded()
        activeGroupPeerDao.save(GroupPeerEntity.fromDomain(peer))
    }

    override suspend fun updatePeer(peer: GroupPeer) {
        updateProfileIdIfNeeded()
        activeGroupPeerDao.update(GroupPeerEntity.fromDomain(peer))
    }

    override suspend fun deletePeer(peer: GroupPeer) {
        updateProfileIdIfNeeded()
        activeGroupPeerDao.delete(GroupPeerEntity.fromDomain(peer))
    }

    override suspend fun deletePeerById(groupChatId: String, peerId: Int) {
        updateProfileIdIfNeeded()
        activeGroupPeerDao.deleteByPeerId(groupChatId, peerId)
    }

    override suspend fun deleteAllPeers(groupChatId: String) {
        updateProfileIdIfNeeded()
        activeGroupPeerDao.deleteAllForGroup(groupChatId)
    }

    override fun getPeers(groupChatId: String): Flow<List<GroupPeer>> = profileIdFlow
        .flatMapLatest { activeGroupPeerDao.loadAllForGroup(groupChatId).map { list -> list.map { it.toDomain() } } }

    override fun getPeer(groupChatId: String, peerId: Int): Flow<GroupPeer?> = profileIdFlow
        .flatMapLatest { activeGroupPeerDao.load(groupChatId, peerId).map { it?.toDomain() } }

    override suspend fun getPeerNameDirect(groupChatId: String, peerId: Int): String? {
        updateProfileIdIfNeeded()
        return activeGroupPeerDao.getPeerNameDirect(groupChatId, peerId)
    }

    override suspend fun peerExistsDirect(groupChatId: String, peerId: Int): Boolean {
        updateProfileIdIfNeeded()
        return activeGroupPeerDao.peerExistsDirect(groupChatId, peerId) > 0
    }

    override suspend fun peerExistsByPublicKey(groupChatId: String, publicKey: String): Boolean {
        updateProfileIdIfNeeded()
        return activeGroupPeerDao.peerExistsByPublicKeyDirect(groupChatId, publicKey) > 0
    }

    override suspend fun deletePeerByPublicKey(groupChatId: String, publicKey: String) {
        updateProfileIdIfNeeded()
        activeGroupPeerDao.deleteByPublicKey(groupChatId, publicKey)
    }

    override suspend fun setPeerName(groupChatId: String, peerId: Int, name: String) {
        updateProfileIdIfNeeded()
        activeGroupPeerDao.setName(groupChatId, peerId, name)
    }

    override suspend fun setPeerRole(groupChatId: String, peerId: Int, role: String) {
        updateProfileIdIfNeeded()
        activeGroupPeerDao.setRole(groupChatId, peerId, role)
    }

    override suspend fun setPeerStatus(groupChatId: String, peerId: Int, status: UserStatus) {
        updateProfileIdIfNeeded()
        activeGroupPeerDao.setStatus(groupChatId, peerId, status)
    }

    override fun peerCount(groupChatId: String): Flow<Int> = profileIdFlow
        .flatMapLatest { activeGroupPeerDao.countForGroup(groupChatId) }

    override suspend fun peerCountDirect(groupChatId: String): Int {
        updateProfileIdIfNeeded()
        return activeGroupPeerDao.countForGroupDirect(groupChatId)
    }
}
