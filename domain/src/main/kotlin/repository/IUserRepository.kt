package ltd.evilcorp.domain.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.domain.model.DomainUser

interface IUserRepository {
    fun get(publicKey: String): Flow<DomainUser?>
    fun add(user: DomainUser)
    fun exists(publicKey: String): Boolean
    fun updateName(publicKey: String, name: String)
    fun updateStatusMessage(publicKey: String, statusMessage: String)
    fun updateStatus(publicKey: String, status: UserStatus)
}
