package ltd.evilcorp.core.db.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.entity.UserEntity
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus

class FakeUserDao : UserDao {
    private val users = MutableStateFlow<Map<String, UserEntity>>(emptyMap())

    override suspend fun save(user: UserEntity) {
        if (users.value.containsKey(user.publicKey)) {
            throw Exception("User already exists")
        }
        users.value = users.value + (user.publicKey to user)
    }

    override suspend fun update(user: UserEntity) {
        users.value = users.value + (user.publicKey to user)
    }

    override suspend fun exists(publicKey: String): Boolean {
        return users.value.containsKey(publicKey)
    }

    override fun load(publicKey: String): Flow<UserEntity?> {
        return users.map { it[publicKey] }
    }

    private suspend fun updateField(publicKey: String, update: (UserEntity) -> UserEntity) {
        val current = users.value[publicKey]
        if (current != null) {
            users.value = users.value + (publicKey to update(current))
        }
    }

    override suspend fun updateName(publicKey: String, name: String) {
        updateField(publicKey) { it.copy(name = name) }
    }

    override suspend fun updateStatusMessage(publicKey: String, statusMessage: String) {
        updateField(publicKey) { it.copy(statusMessage = statusMessage) }
    }

    override suspend fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus) {
        updateField(publicKey) { it.copy(connectionStatus = connectionStatus) }
    }

    override suspend fun updateStatus(publicKey: String, status: UserStatus) {
        updateField(publicKey) { it.copy(status = status) }
    }
}
