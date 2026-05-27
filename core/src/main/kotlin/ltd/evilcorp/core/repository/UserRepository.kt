package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.UserDao
import ltd.evilcorp.core.db.entity.UserEntity
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.User
import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.domain.repository.IUserRepository

@Singleton
class UserRepository @Inject constructor(private val userDao: UserDao) : IUserRepository {
    override fun exists(publicKey: String): Boolean = userDao.exists(publicKey)

    override fun add(user: User) = userDao.save(UserEntity.fromDomain(user))

    override fun update(user: User) = userDao.update(UserEntity.fromDomain(user))

    override fun get(publicKey: String): Flow<User?> = userDao.load(publicKey).map { it?.toDomain() }

    override fun updateName(publicKey: String, name: String) = userDao.updateName(publicKey, name)

    override fun updateStatusMessage(publicKey: String, statusMessage: String) =
        userDao.updateStatusMessage(publicKey, statusMessage)

    override fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus) =
        userDao.updateConnection(publicKey, connectionStatus)

    override fun updateStatus(publicKey: String, status: UserStatus) = userDao.updateStatus(publicKey, status)
}
