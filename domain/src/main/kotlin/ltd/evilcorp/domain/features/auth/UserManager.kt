package ltd.evilcorp.domain.features.auth

import javax.inject.Inject
import ltd.evilcorp.domain.features.auth.repository.IUserRepository
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.core.network.IToxProfile

open class UserManager @Inject constructor(
    private val userRepository: IUserRepository,
    private val tox: IToxProfile,
) {
    open fun get(publicKey: PublicKey) = userRepository.get(publicKey.string())

    open suspend fun create(user: User): Result<Unit> = runCatching {
        userRepository.add(user)
        tox.setName(user.name)
        tox.setStatusMessage(user.statusMessage)
    }

    open suspend fun verifyExists(publicKey: PublicKey): Result<Unit> = runCatching {
        if (!userRepository.exists(publicKey.string())) {
            val name = tox.getName()
            val statusMessage = tox.getStatusMessage()
            val user = User(publicKey.string(), name, statusMessage)
            userRepository.add(user)
        }
    }

    suspend fun setName(name: String): Result<Unit> = runCatching {
        tox.setName(name)
        userRepository.updateName(tox.publicKey.string(), name)
    }

    suspend fun setStatusMessage(statusMessage: String): Result<Unit> = runCatching {
        tox.setStatusMessage(statusMessage)
        userRepository.updateStatusMessage(tox.publicKey.string(), statusMessage)
    }

    suspend fun setStatus(status: UserStatus): Result<Unit> = runCatching {
        tox.setStatus(status)
        userRepository.updateStatus(tox.publicKey.string(), status)
    }
}
