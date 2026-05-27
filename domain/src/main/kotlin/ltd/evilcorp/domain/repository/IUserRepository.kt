package ltd.evilcorp.domain.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.User
import ltd.evilcorp.domain.model.UserStatus

interface IUserRepository {
    fun get(publicKey: String): Flow<User?>
    fun add(user: User)
    fun exists(publicKey: String): Boolean
    fun updateName(publicKey: String, name: String)
    fun updateStatusMessage(publicKey: String, statusMessage: String)
    fun updateStatus(publicKey: String, status: UserStatus)
    fun update(user: User)
    fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus)
}

