package ltd.evilcorp.domain.feature

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.repository.IUserRepository
import ltd.evilcorp.domain.model.User
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.domain.tox.ITox

open class UserManager @Inject constructor(
    private val scope: CoroutineScope,
    private val userRepository: IUserRepository,
    private val tox: ITox,
) {
    open fun get(publicKey: PublicKey) = userRepository.get(publicKey.string())

    open fun create(user: User) = scope.launch {
        userRepository.add(user)
        tox.setName(user.name)
        tox.setStatusMessage(user.statusMessage)
    }

    open fun verifyExists(publicKey: PublicKey) = scope.launch {
        if (!userRepository.exists(publicKey.string())) {
            val name = tox.getName()
            val statusMessage = tox.getStatusMessage()
            val user = User(publicKey.string(), name, statusMessage)
            userRepository.add(user)
        }
    }

    fun setName(name: String) = scope.launch {
        tox.setName(name)
        userRepository.updateName(tox.publicKey.string(), name)
    }

    fun setStatusMessage(statusMessage: String) = scope.launch {
        tox.setStatusMessage(statusMessage)
        userRepository.updateStatusMessage(tox.publicKey.string(), statusMessage)
    }

    fun setStatus(status: UserStatus) = scope.launch {
        tox.setStatus(status)
        userRepository.updateStatus(tox.publicKey.string(), status)
    }
}

