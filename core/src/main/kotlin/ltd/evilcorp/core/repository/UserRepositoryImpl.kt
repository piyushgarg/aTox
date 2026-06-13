package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ltd.evilcorp.core.db.dao.UserDao
import ltd.evilcorp.core.db.entity.UserEntity
import ltd.evilcorp.domain.core.di.IoDispatcher
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.auth.repository.IUserRepository

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dbProvider: javax.inject.Provider<ltd.evilcorp.core.db.Database>? = null
) : IUserRepository {
    private val activeUserDao: UserDao get() = dbProvider?.get()?.userDao() ?: userDao

    override suspend fun exists(publicKey: String): Boolean = withContext(ioDispatcher) {
        activeUserDao.exists(publicKey)
    }

    override suspend fun add(user: User) = withContext(ioDispatcher) {
        activeUserDao.save(UserEntity.fromDomain(user))
    }

    override suspend fun update(user: User) = withContext(ioDispatcher) {
        activeUserDao.update(UserEntity.fromDomain(user))
    }

    override fun get(publicKey: String): Flow<User?> = activeUserDao.load(publicKey).map { it?.toDomain() }

    override suspend fun updateName(publicKey: String, name: String) = withContext(ioDispatcher) {
        activeUserDao.updateName(publicKey, name)
    }

    override suspend fun updateStatusMessage(publicKey: String, statusMessage: String) = withContext(ioDispatcher) {
        activeUserDao.updateStatusMessage(publicKey, statusMessage)
    }

    override suspend fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus) = withContext(ioDispatcher) {
        activeUserDao.updateConnection(publicKey, connectionStatus)
    }

    override suspend fun updateStatus(publicKey: String, status: UserStatus) = withContext(ioDispatcher) {
        activeUserDao.updateStatus(publicKey, status)
    }
}

