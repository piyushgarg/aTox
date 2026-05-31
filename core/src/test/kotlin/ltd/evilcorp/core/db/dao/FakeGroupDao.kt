package ltd.evilcorp.core.db.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.entity.GroupEntity
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState

class FakeGroupDao : GroupDao {
    private val groups = MutableStateFlow<Map<String, GroupEntity>>(emptyMap())

    override suspend fun save(group: GroupEntity) {
        groups.value = groups.value + (group.chatId to group)
    }

    override suspend fun update(group: GroupEntity) {
        groups.value = groups.value + (group.chatId to group)
    }

    override suspend fun delete(group: GroupEntity) {
        groups.value = groups.value - group.chatId
    }

    override fun load(chatId: String): Flow<GroupEntity?> {
        return groups.map { it[chatId] }
    }

    override suspend fun loadDirect(chatId: String): GroupEntity? {
        return groups.value[chatId]
    }

    override suspend fun exists(chatId: String): Int {
        return if (groups.value.containsKey(chatId)) 1 else 0
    }

    override fun loadAll(): Flow<List<GroupEntity>> {
        return groups.map { it.values.toList() }
    }

    private suspend fun updateField(chatId: String, update: (GroupEntity) -> GroupEntity) {
        val current = groups.value[chatId]
        if (current != null) {
            groups.value = groups.value + (chatId to update(current))
        }
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

    override suspend fun deleteByChatId(chatId: String) {
        groups.value = groups.value - chatId
    }
}
