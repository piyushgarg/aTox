package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.dao.ContactDao
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository

@Singleton
class ContactRepositoryImpl @Inject constructor(private val dao: ContactDao) : IContactRepository {
    override suspend fun exists(publicKey: String): Boolean = dao.exists(publicKey)
    override suspend fun add(contact: Contact) = dao.save(ContactEntity.fromDomain(contact))
    override suspend fun update(contact: Contact) = dao.update(ContactEntity.fromDomain(contact))
    override suspend fun delete(contact: Contact) = dao.delete(ContactEntity.fromDomain(contact))
    override fun get(publicKey: String): Flow<Contact?> = dao.load(publicKey).map { it?.toDomain() }
    override fun getAll(): Flow<List<Contact>> = dao.loadAll().map { list -> list.map { it.toDomain() } }
    override suspend fun resetTransientData() = dao.resetTransientData()
 
    override suspend fun setName(publicKey: String, name: String) = dao.setName(publicKey, name)
    override suspend fun setStatusMessage(publicKey: String, statusMessage: String) = dao.setStatusMessage(publicKey, statusMessage)
    override suspend fun setLastMessage(publicKey: String, lastMessage: Long) = dao.setLastMessage(publicKey, lastMessage)
    override suspend fun setUserStatus(publicKey: String, status: UserStatus) = dao.setUserStatus(publicKey, status)
    override suspend fun setConnectionStatus(publicKey: String, status: ConnectionStatus) = dao.setConnectionStatus(publicKey, status)
    override suspend fun setTyping(publicKey: String, typing: Boolean) = dao.setTyping(publicKey, typing)
    override suspend fun setAvatarUri(publicKey: String, uri: String) = dao.setAvatarUri(publicKey, uri)
    override suspend fun setHasUnreadMessages(publicKey: String, anyUnread: Boolean) = dao.setHasUnreadMessages(publicKey, anyUnread)
    override suspend fun setDraftMessage(publicKey: String, draft: String) = dao.setDraftMessage(publicKey, draft)
    override suspend fun setLastOnline(publicKey: String, lastOnline: Long) = dao.setLastOnline(publicKey, lastOnline)
}
