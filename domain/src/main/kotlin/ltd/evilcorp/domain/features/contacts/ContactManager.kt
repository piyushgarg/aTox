package ltd.evilcorp.domain.features.contacts

import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.IToxProfile
import ltd.evilcorp.domain.core.network.ToxID

class ContactManager @Inject constructor(
    private val contactRepository: IContactRepository,
    private val tox: IToxProfile,
) {
    fun get(publicKey: PublicKey) = contactRepository.get(publicKey.string())
    fun getAll() = contactRepository.getAll()

    suspend fun add(toxID: ToxID, message: String) = withContext(Dispatchers.IO) {
        val publicKeyTxt = toxID.toPublicKey().string()
        tox.addContact(toxID, message)
        contactRepository.add(Contact(publicKeyTxt))
        contactRepository.setLastMessage(publicKeyTxt, System.currentTimeMillis())
    }

    suspend fun delete(publicKey: PublicKey) = withContext(Dispatchers.IO) {
        tox.deleteContact(publicKey)
        contactRepository.delete(Contact(publicKey.string()))
    }

    suspend fun setDraft(pk: PublicKey, draft: String) = withContext(Dispatchers.IO) {
        contactRepository.setDraftMessage(pk.string(), draft)
    }
}

