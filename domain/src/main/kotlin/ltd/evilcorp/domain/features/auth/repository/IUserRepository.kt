package ltd.evilcorp.domain.features.auth.repository

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.UserStatus

interface IUserRepository {
    fun get(publicKey: String): Flow<User?>
    suspend fun add(user: User)
    suspend fun exists(publicKey: String): Boolean
    suspend fun updateName(publicKey: String, name: String)
    suspend fun updateStatusMessage(publicKey: String, statusMessage: String)
    suspend fun updateStatus(publicKey: String, status: UserStatus)
    suspend fun update(user: User)
    suspend fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus)
}

