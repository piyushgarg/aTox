package ltd.evilcorp.domain.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.model.Contact

interface IContactRepository {
    fun exists(publicKey: String): Boolean
    fun add(contact: Contact)
    fun update(contact: Contact)
    fun delete(contact: Contact)
    fun get(publicKey: String): Flow<Contact?>
    fun getAll(): Flow<List<Contact>>
    fun resetTransientData()
    fun setName(publicKey: String, name: String)
    fun setStatusMessage(publicKey: String, statusMessage: String)
    fun setLastMessage(publicKey: String, lastMessage: Long)
    fun setUserStatus(publicKey: String, status: UserStatus)
    fun setConnectionStatus(publicKey: String, status: ConnectionStatus)
    fun setTyping(publicKey: String, typing: Boolean)
    fun setAvatarUri(publicKey: String, uri: String)
    fun setHasUnreadMessages(publicKey: String, anyUnread: Boolean)
    fun setDraftMessage(publicKey: String, draft: String)
    fun setLastOnline(publicKey: String, lastOnline: Long)
}
