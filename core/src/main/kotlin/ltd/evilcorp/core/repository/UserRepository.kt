package ltd.evilcorp.core.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.core.db.UserDao
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.User
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.domain.repository.IUserRepository

@Singleton
class UserRepository @Inject constructor(private val userDao: UserDao) : IUserRepository {
    override fun exists(publicKey: String): Boolean = userDao.exists(publicKey)

    override fun add(user: User) = userDao.save(user)

    fun update(user: User) = userDao.update(user)

    override fun get(publicKey: String): Flow<User?> = userDao.load(publicKey)

    override fun updateName(publicKey: String, name: String) = userDao.updateName(publicKey, name)

    override fun updateStatusMessage(publicKey: String, statusMessage: String) =
        userDao.updateStatusMessage(publicKey, statusMessage)

    fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus) =
        userDao.updateConnection(publicKey, connectionStatus)

    override fun updateStatus(publicKey: String, status: UserStatus) = userDao.updateStatus(publicKey, status)
}
