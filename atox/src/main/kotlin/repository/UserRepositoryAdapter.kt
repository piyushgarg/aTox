package ltd.evilcorp.atox.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.repository.UserRepository
import ltd.evilcorp.domain.model.DomainUser
import ltd.evilcorp.domain.model.toDb
import ltd.evilcorp.domain.model.toDomain
import ltd.evilcorp.domain.repository.IUserRepository

@Singleton
class UserRepositoryAdapter @Inject constructor(
    private val userRepository: UserRepository
) : IUserRepository {
    override fun get(publicKey: String): Flow<DomainUser?> = userRepository.get(publicKey).map { it?.toDomain() }
    override fun add(user: DomainUser) = userRepository.add(user.toDb())
    override fun exists(publicKey: String): Boolean = userRepository.exists(publicKey)
    override fun updateName(publicKey: String, name: String) = userRepository.updateName(publicKey, name)
    override fun updateStatusMessage(publicKey: String, statusMessage: String) = userRepository.updateStatusMessage(publicKey, statusMessage)
    override fun updateStatus(publicKey: String, status: UserStatus) = userRepository.updateStatus(publicKey, status)
}
