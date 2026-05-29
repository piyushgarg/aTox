package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.core.db.dao.UserDao
import ltd.evilcorp.core.db.entity.UserEntity
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.auth.repository.IUserRepository

@Singleton
class UserRepositoryImpl @Inject constructor(private val userDao: UserDao) : IUserRepository {
    override suspend fun exists(publicKey: String): Boolean = withContext(Dispatchers.IO) {
        userDao.exists(publicKey)
    }

    override suspend fun add(user: User) = withContext(Dispatchers.IO) {
        userDao.save(UserEntity.fromDomain(user))
    }

    override suspend fun update(user: User) = withContext(Dispatchers.IO) {
        userDao.update(UserEntity.fromDomain(user))
    }

    override fun get(publicKey: String): Flow<User?> = userDao.load(publicKey).map { it?.toDomain() }

    override suspend fun updateName(publicKey: String, name: String) = withContext(Dispatchers.IO) {
        userDao.updateName(publicKey, name)
    }

    override suspend fun updateStatusMessage(publicKey: String, statusMessage: String) = withContext(Dispatchers.IO) {
        userDao.updateStatusMessage(publicKey, statusMessage)
    }

    override suspend fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus) = withContext(Dispatchers.IO) {
        userDao.updateConnection(publicKey, connectionStatus)
    }

    override suspend fun updateStatus(publicKey: String, status: UserStatus) = withContext(Dispatchers.IO) {
        userDao.updateStatus(publicKey, status)
    }
}
