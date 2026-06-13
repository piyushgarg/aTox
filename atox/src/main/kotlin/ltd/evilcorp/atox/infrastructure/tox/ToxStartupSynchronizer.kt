package ltd.evilcorp.atox.infrastructure.tox

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.auth.repository.IUserRepository
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.features.group.usecase.ReconnectGroupsUseCase

import android.util.Log

class ToxStartupSynchronizer @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: IContactRepository,
    private val userRepository: IUserRepository,
    private val tox: ITox,
    private val reconnectGroupsUseCase: ReconnectGroupsUseCase,
) {
    fun synchronizeAfterStart() {
        scope.launch {
            contactRepository.resetTransientData()

            for ((publicKey, _) in tox.getContacts()) {
                val pkStr = publicKey.string()
                if (!contactRepository.exists(pkStr)) {
                    // Leftover temporary bootstrap friend from a previous run: clean it up from JNI instead of adding to DB
                    try {
                        tox.deleteContact(publicKey)
                        Log.i("ToxStartupSynchronizer", "Cleaned up leftover bootstrap contact from JNI: $pkStr")
                    } catch (e: Exception) {
                        Log.w("ToxStartupSynchronizer", "Failed to clean up leftover bootstrap contact: $pkStr", e)
                    }
                }
            }

            userRepository.updateConnection(tox.publicKey.string(), ConnectionStatus.None)
            reconnectGroupsUseCase.execute()
        }
    }
}
