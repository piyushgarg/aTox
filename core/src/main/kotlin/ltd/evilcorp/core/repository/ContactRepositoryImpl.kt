package ltd.evilcorp.core.repository

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ltd.evilcorp.core.db.dao.ContactDao
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.core.profile.ProfileManager

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao,
    private val context: android.content.Context? = null,
    private val dbProvider: javax.inject.Provider<ltd.evilcorp.core.db.Database>? = null
) : IContactRepository {
    private val internalCache = ConcurrentHashMap<String, Contact>()
    private var cachedProfileId: String? = null

    private val cache: ConcurrentHashMap<String, Contact> get() {
        val currentProfileId = context?.let { ProfileManager.getActiveProfileId(it) } ?: ProfileManager.DEFAULT_PROFILE_ID
        if (cachedProfileId != currentProfileId) {
            internalCache.clear()
            cachedProfileId = currentProfileId
        }
        return internalCache
    }
    private val activeDao: ContactDao get() = dbProvider?.get()?.contactDao() ?: dao

    override suspend fun exists(publicKey: String): Boolean {
        if (cache.containsKey(publicKey)) return true
        return activeDao.exists(publicKey)
    }

    override suspend fun add(contact: Contact) {
        cache[contact.publicKey] = contact
        activeDao.save(ContactEntity.fromDomain(contact))
    }

    override suspend fun update(contact: Contact) {
        cache[contact.publicKey] = contact
        activeDao.update(ContactEntity.fromDomain(contact))
    }

    override suspend fun delete(contact: Contact) {
        cache.remove(contact.publicKey)
        activeDao.delete(ContactEntity.fromDomain(contact))
    }

    override fun get(publicKey: String): Flow<Contact?> = activeDao.load(publicKey)
        .map { it?.toDomain() }
        .onEach { contact ->
            if (contact != null) {
                cache[publicKey] = contact
            } else {
                cache.remove(publicKey)
            }
        }

    override fun getAll(): Flow<List<Contact>> = activeDao.loadAll()
        .map { list -> list.map { it.toDomain() } }
        .onEach { list ->
            list.forEach { contact ->
                cache[contact.publicKey] = contact
            }
        }

    override suspend fun resetTransientData() {
        cache.clear()
        activeDao.resetTransientData()
    }

    override suspend fun setName(publicKey: String, name: String) {
        cache[publicKey]?.let { cache[publicKey] = it.copy(name = name) }
        activeDao.setName(publicKey, name)
    }

    override suspend fun setStatusMessage(publicKey: String, statusMessage: String) {
        cache[publicKey]?.let { cache[publicKey] = it.copy(statusMessage = statusMessage) }
        activeDao.setStatusMessage(publicKey, statusMessage)
    }

    override suspend fun setLastMessage(publicKey: String, lastMessage: Long) {
        cache[publicKey]?.let { cache[publicKey] = it.copy(lastMessage = lastMessage) }
        activeDao.setLastMessage(publicKey, lastMessage)
    }

    override suspend fun setUserStatus(publicKey: String, status: UserStatus) {
        cache[publicKey]?.let { cache[publicKey] = it.copy(status = status) }
        activeDao.setUserStatus(publicKey, status)
    }

    override suspend fun setConnectionStatus(publicKey: String, status: ConnectionStatus) {
        cache[publicKey]?.let { cache[publicKey] = it.copy(connectionStatus = status) }
        activeDao.setConnectionStatus(publicKey, status)
    }

    override suspend fun setTyping(publicKey: String, typing: Boolean) {
        cache[publicKey]?.let { cache[publicKey] = it.copy(typing = typing) }
        activeDao.setTyping(publicKey, typing)
    }

    override suspend fun setAvatarUri(publicKey: String, uri: String) {
        cache[publicKey]?.let { cache[publicKey] = it.copy(avatarUri = uri) }
        activeDao.setAvatarUri(publicKey, uri)
    }

    override suspend fun setHasUnreadMessages(publicKey: String, anyUnread: Boolean) {
        cache[publicKey]?.let { cache[publicKey] = it.copy(hasUnreadMessages = anyUnread) }
        activeDao.setHasUnreadMessages(publicKey, anyUnread)
    }

    override suspend fun setDraftMessage(publicKey: String, draft: String) {
        cache[publicKey]?.let { cache[publicKey] = it.copy(draftMessage = draft) }
        activeDao.setDraftMessage(publicKey, draft)
    }

    override suspend fun setLastOnline(publicKey: String, lastOnline: Long) {
        cache[publicKey]?.let { cache[publicKey] = it.copy(lastOnline = lastOnline) }
        activeDao.setLastOnline(publicKey, lastOnline)
    }
}
