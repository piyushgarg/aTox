package ltd.evilcorp.domain.features.contacts.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.contacts.model.Contact

@Suppress("ComplexInterface")
interface IContactRepository {
    suspend fun exists(publicKey: String): Boolean
    suspend fun add(contact: Contact)
    suspend fun update(contact: Contact)
    suspend fun delete(contact: Contact)
    fun get(publicKey: String): Flow<Contact?>
    fun getAll(): Flow<List<Contact>>
    suspend fun resetTransientData()
    suspend fun setName(publicKey: String, name: String)
    suspend fun setStatusMessage(publicKey: String, statusMessage: String)
    suspend fun setLastMessage(publicKey: String, lastMessage: Long)
    suspend fun setUserStatus(publicKey: String, status: UserStatus)
    suspend fun setConnectionStatus(publicKey: String, status: ConnectionStatus)
    suspend fun setTyping(publicKey: String, typing: Boolean)
    suspend fun setAvatarUri(publicKey: String, uri: String)
    suspend fun setHasUnreadMessages(publicKey: String, anyUnread: Boolean)
    suspend fun setDraftMessage(publicKey: String, draft: String)
    suspend fun setLastOnline(publicKey: String, lastOnline: Long)
}
