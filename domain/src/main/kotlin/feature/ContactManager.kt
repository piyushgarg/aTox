package ltd.evilcorp.domain.feature

import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.repository.IContactRepository
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.domain.tox.ITox
import ltd.evilcorp.core.tox.ToxID

class ContactManager @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: IContactRepository,
    private val tox: ITox,
) {
    fun get(publicKey: PublicKey) = contactRepository.get(publicKey.string())
    fun getAll() = contactRepository.getAll()

    fun add(toxID: ToxID, message: String) = scope.launch {
        val publicKeyTxt = toxID.toPublicKey().string()
        tox.addContact(toxID, message)
        contactRepository.add(Contact(publicKeyTxt))
        contactRepository.setLastMessage(publicKeyTxt, Date().time)
    }

    fun delete(publicKey: PublicKey) = scope.launch {
        tox.deleteContact(publicKey)
        contactRepository.delete(Contact(publicKey.string()))
    }

    fun setDraft(pk: PublicKey, draft: String) = scope.launch {
        contactRepository.setDraftMessage(pk.string(), draft)
    }
}

