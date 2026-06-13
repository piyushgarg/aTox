package ltd.evilcorp.domain.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.auth.repository.IUserRepository

class FakeUserRepository : IUserRepository {
    private val users = MutableStateFlow<Map<String, User>>(emptyMap())

    override fun get(publicKey: String): Flow<User?> {
        return users.map { it[publicKey] }
    }

    override suspend fun add(user: User) {
        users.value = users.value + (user.publicKey to user)
    }

    override suspend fun exists(publicKey: String): Boolean {
        return users.value.containsKey(publicKey)
    }

    override suspend fun update(user: User) {
        users.value = users.value + (user.publicKey to user)
    }

    override suspend fun updateName(publicKey: String, name: String) {
        updateField(publicKey) { it.copy(name = name) }
    }

    override suspend fun updateStatusMessage(publicKey: String, statusMessage: String) {
        updateField(publicKey) { it.copy(statusMessage = statusMessage) }
    }

    override suspend fun updateStatus(publicKey: String, status: UserStatus) {
        updateField(publicKey) { it.copy(status = status) }
    }

    override suspend fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus) {
        updateField(publicKey) { it.copy(connectionStatus = connectionStatus) }
    }

    private fun updateField(publicKey: String, update: (User) -> User) {
        val current = users.value[publicKey] ?: User(publicKey, "", "", UserStatus.None, ConnectionStatus.None)
        users.value = users.value + (publicKey to update(current))
    }
}
