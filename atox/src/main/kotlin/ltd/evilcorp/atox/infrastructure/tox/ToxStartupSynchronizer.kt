package ltd.evilcorp.atox.infrastructure.tox

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.auth.repository.IUserRepository
import ltd.evilcorp.domain.core.network.ITox

class ToxStartupSynchronizer @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: IContactRepository,
    private val userRepository: IUserRepository,
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
