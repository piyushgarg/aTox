package ltd.evilcorp.atox.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.domain.model.DomainContact
import ltd.evilcorp.domain.model.toDb
import ltd.evilcorp.domain.model.toDomain
import ltd.evilcorp.domain.repository.IContactRepository

@Singleton
class ContactRepositoryAdapter @Inject constructor(
    private val contactRepository: ContactRepository
) : IContactRepository {
    override fun exists(publicKey: String): Boolean = contactRepository.exists(publicKey)
    override fun add(contact: DomainContact) = contactRepository.add(contact.toDb())
    override fun update(contact: DomainContact) = contactRepository.update(contact.toDb())
    override fun delete(contact: DomainContact) = contactRepository.delete(contact.toDb())
    override fun get(publicKey: String): Flow<DomainContact?> = contactRepository.get(publicKey).map { it?.toDomain() }
    override fun getAll(): Flow<List<DomainContact>> = contactRepository.getAll().map { list -> list.map { it.toDomain() } }
    override fun resetTransientData() = contactRepository.resetTransientData()

    override fun setName(publicKey: String, name: String) = contactRepository.setName(publicKey, name)
    override fun setStatusMessage(publicKey: String, statusMessage: String) = contactRepository.setStatusMessage(publicKey, statusMessage)
    override fun setLastMessage(publicKey: String, lastMessage: Long) = contactRepository.setLastMessage(publicKey, lastMessage)
    override fun setUserStatus(publicKey: String, status: UserStatus) = contactRepository.setUserStatus(publicKey, status)
    override fun setConnectionStatus(publicKey: String, status: ConnectionStatus) = contactRepository.setConnectionStatus(publicKey, status)
    override fun setTyping(publicKey: String, typing: Boolean) = contactRepository.setTyping(publicKey, typing)
    override fun setAvatarUri(publicKey: String, uri: String) = contactRepository.setAvatarUri(publicKey, uri)
    override fun setHasUnreadMessages(publicKey: String, anyUnread: Boolean) = contactRepository.setHasUnreadMessages(publicKey, anyUnread)
    override fun setDraftMessage(publicKey: String, draft: String) = contactRepository.setDraftMessage(publicKey, draft)
    override fun setLastOnline(publicKey: String, lastOnline: Long) = contactRepository.setLastOnline(publicKey, lastOnline)
}
