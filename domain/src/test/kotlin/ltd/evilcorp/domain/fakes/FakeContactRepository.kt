package ltd.evilcorp.domain.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository

class FakeContactRepository : IContactRepository {
    private val contacts = MutableStateFlow<Map<String, Contact>>(emptyMap())

    override suspend fun exists(publicKey: String): Boolean {
        return contacts.value.containsKey(publicKey)
    }

    override suspend fun add(contact: Contact) {
        contacts.value = contacts.value + (contact.publicKey to contact)
    }

    override suspend fun update(contact: Contact) {
        contacts.value = contacts.value + (contact.publicKey to contact)
    }

    override suspend fun delete(contact: Contact) {
        contacts.value = contacts.value - contact.publicKey
    }

    override fun get(publicKey: String): Flow<Contact?> {
        return contacts.map { it[publicKey] }
    }

    override fun getAll(): Flow<List<Contact>> {
        return contacts.map { it.values.toList() }
    }

    override suspend fun resetTransientData() {
        contacts.value = contacts.value.mapValues { (_, contact) ->
            contact.copy(
                connectionStatus = ConnectionStatus.None,
                typing = false
            )
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

    override suspend fun setConnectionStatus(publicKey: String, status: ConnectionStatus) {
        updateField(publicKey) { it.copy(connectionStatus = status) }
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

    private fun updateField(publicKey: String, update: (Contact) -> Contact) {
        val current = contacts.value[publicKey] ?: Contact(publicKey)
        contacts.value = contacts.value + (publicKey to update(current))
    }
}
