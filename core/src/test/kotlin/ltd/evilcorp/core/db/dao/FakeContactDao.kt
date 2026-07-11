package ltd.evilcorp.core.db.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus

class FakeContactDao : ContactDao {
    private val contacts = MutableStateFlow<Map<String, ContactEntity>>(emptyMap())

    override suspend fun save(contact: ContactEntity) {
        contacts.value = contacts.value + (contact.publicKey to contact)
    }

    override suspend fun update(contact: ContactEntity) {
        contacts.value = contacts.value + (contact.publicKey to contact)
    }

    override suspend fun delete(contact: ContactEntity) {
        contacts.value = contacts.value - contact.publicKey
    }

    override suspend fun exists(publicKey: String): Boolean {
        return contacts.value.containsKey(publicKey)
    }

    override fun load(publicKey: String): Flow<ContactEntity?> {
        return contacts.map { it[publicKey] }
    }

    override fun loadAll(): Flow<List<ContactEntity>> {
        return contacts.map { it.values.toList() }
    }

    override suspend fun loadAllBlocking(): List<ContactEntity> {
        return contacts.value.values.toList()
    }

    override suspend fun saveAll(contacts: List<ContactEntity>) {
        val newMap = this.contacts.value.toMutableMap()
        contacts.forEach { newMap[it.publicKey] = it }
        this.contacts.value = newMap
    }

    override suspend fun resetTransientData(status: ConnectionStatus, typing: Boolean) {
        val newMap = this.contacts.value.toMutableMap()
        newMap.forEach { (key, contact) ->
            newMap[key] = contact.copy(connectionStatus = status, typing = typing)
        }
        this.contacts.value = newMap
    }

    private suspend fun updateField(publicKey: String, update: (ContactEntity) -> ContactEntity) {
        val current = contacts.value[publicKey]
        if (current != null) {
            contacts.value = contacts.value + (publicKey to update(current))
        }
    }

    override suspend fun setName(publicKey: String, name: String) {
        updateField(publicKey) { it.copy(name = name) }
    }

    override suspend fun setStatusMessage(publicKey: String, statusMessage: String) {
        updateField(publicKey) { it.copy(statusMessage = statusMessage) }
    }

    override suspend fun setLastMessage(publicKey: String, lastMessage: Long) {
        updateField(publicKey) { it.copy(lastMessage = lastMessage) }
    }

    override suspend fun setUserStatus(publicKey: String, status: UserStatus) {
        updateField(publicKey) { it.copy(status = status) }
    }

    override suspend fun setConnectionStatus(publicKey: String, connectionStatus: ConnectionStatus) {
        updateField(publicKey) { it.copy(connectionStatus = connectionStatus) }
    }

    override suspend fun setTyping(publicKey: String, typing: Boolean) {
        updateField(publicKey) { it.copy(typing = typing) }
    }

    override suspend fun setAvatarUri(publicKey: String, uri: String) {
        updateField(publicKey) { it.copy(avatarUri = uri) }
    }

    override suspend fun setHasUnreadMessages(publicKey: String, anyUnread: Boolean) {
        updateField(publicKey) { it.copy(hasUnreadMessages = anyUnread) }
    }

    override suspend fun setDraftMessage(publicKey: String, draft: String) {
        updateField(publicKey) { it.copy(draftMessage = draft) }
    }

    override suspend fun setLastOnline(publicKey: String, lastOnline: Long) {
        updateField(publicKey) { it.copy(lastOnline = lastOnline) }
    }
}
