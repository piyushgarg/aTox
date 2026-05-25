package ltd.evilcorp.domain.feature

import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.repository.IContactRepository
import ltd.evilcorp.domain.model.DomainContact
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.domain.tox.Tox
import ltd.evilcorp.core.tox.ToxID

class ContactManager @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: IContactRepository,
    private val tox: Tox,
) {
    fun get(publicKey: PublicKey) = contactRepository.get(publicKey.string())
    fun getAll() = contactRepository.getAll()

    fun add(toxID: ToxID, message: String) = scope.launch {
        val publicKeyTxt = toxID.toPublicKey().string()
        tox.addContact(toxID, message)
        contactRepository.add(DomainContact(publicKeyTxt))
        contactRepository.setLastMessage(publicKeyTxt, Date().time)
    }

    fun delete(publicKey: PublicKey) = scope.launch {
        tox.deleteContact(publicKey)
        contactRepository.delete(DomainContact(publicKey.string()))
    }

    fun setDraft(pk: PublicKey, draft: String) = scope.launch {
        contactRepository.setDraftMessage(pk.string(), draft)
    }
}

