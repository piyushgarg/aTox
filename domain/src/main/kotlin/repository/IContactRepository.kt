package ltd.evilcorp.domain.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.domain.model.DomainContact

interface IContactRepository {
    fun exists(publicKey: String): Boolean
    fun add(contact: DomainContact)
    fun update(contact: DomainContact)
    fun delete(contact: DomainContact)
    fun get(publicKey: String): Flow<DomainContact?>
    fun getAll(): Flow<List<DomainContact>>
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
