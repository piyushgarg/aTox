package ltd.evilcorp.atox.tox

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.UserRepository
import ltd.evilcorp.domain.tox.ITox

class ToxStartupSynchronizer @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: ContactRepository,
    private val userRepository: UserRepository,
    private val tox: ITox,
) {
    fun synchronizeAfterStart() {
        scope.launch {
            contactRepository.resetTransientData()

            for ((publicKey, _) in tox.getContacts()) {
                if (!contactRepository.exists(publicKey.string())) {
                    contactRepository.add(Contact(publicKey.string()))
                }
            }

            userRepository.updateConnection(tox.publicKey.string(), ConnectionStatus.None)
        }
    }
}
