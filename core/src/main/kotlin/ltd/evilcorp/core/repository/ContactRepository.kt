package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.ContactDao
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.domain.repository.IContactRepository

@Singleton
class ContactRepository @Inject constructor(private val dao: ContactDao) : IContactRepository {
    override fun exists(publicKey: String): Boolean = dao.exists(publicKey)
    override fun add(contact: Contact) = dao.save(ContactEntity.fromDomain(contact))
    override fun update(contact: Contact) = dao.update(ContactEntity.fromDomain(contact))
    override fun delete(contact: Contact) = dao.delete(ContactEntity.fromDomain(contact))
    override fun get(publicKey: String): Flow<Contact?> = dao.load(publicKey).map { it?.toDomain() }
    override fun getAll(): Flow<List<Contact>> = dao.loadAll().map { list -> list.map { it.toDomain() } }
    override fun resetTransientData() = dao.resetTransientData()

    override fun setName(publicKey: String, name: String) = dao.setName(publicKey, name)
    override fun setStatusMessage(publicKey: String, statusMessage: String) = dao.setStatusMessage(publicKey, statusMessage)
    override fun setLastMessage(publicKey: String, lastMessage: Long) = dao.setLastMessage(publicKey, lastMessage)
    override fun setUserStatus(publicKey: String, status: UserStatus) = dao.setUserStatus(publicKey, status)
    override fun setConnectionStatus(publicKey: String, status: ConnectionStatus) = dao.setConnectionStatus(publicKey, status)
    override fun setTyping(publicKey: String, typing: Boolean) = dao.setTyping(publicKey, typing)
    override fun setAvatarUri(publicKey: String, uri: String) = dao.setAvatarUri(publicKey, uri)
    override fun setHasUnreadMessages(publicKey: String, anyUnread: Boolean) = dao.setHasUnreadMessages(publicKey, anyUnread)
    override fun setDraftMessage(publicKey: String, draft: String) = dao.setDraftMessage(publicKey, draft)
    override fun setLastOnline(publicKey: String, lastOnline: Long) = dao.setLastOnline(publicKey, lastOnline)
}
