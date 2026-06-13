package ltd.evilcorp.core.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.dao.FakeUserDao
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    private lateinit var dao: FakeUserDao
    private lateinit var repository: UserRepositoryImpl

    private val testUser = User(
        publicKey = "1234",
        name = "Alice",
        statusMessage = "Brought to you by aTox",
        status = UserStatus.Away,
        connectionStatus = ConnectionStatus.UDP,
        password = "secret_password"
    )

    @BeforeTest
    fun setUp() {
        dao = FakeUserDao()
        repository = UserRepositoryImpl(dao, Dispatchers.Unconfined)
    }

    @Test
    fun testExists_nonExistent_returnsFalse() = runTest {
        assertFalse(repository.exists(testUser.publicKey))
    }

    @Test
    fun testExists_existent_returnsTrue() = runTest {
        repository.add(testUser)
        assertTrue(repository.exists(testUser.publicKey))
    }

    @Test
    fun testAdd_and_get_returnsCorrectUser() = runTest {
        repository.add(testUser)
        val loaded = repository.get(testUser.publicKey).first()
        assertEquals(testUser, loaded)
    }

    @Test
    fun testGet_nonExistent_returnsNull() = runTest {
        val loaded = repository.get("non-existent").first()
        assertNull(loaded)
    }

    @Test
    fun testUpdate_modifiesDatabaseCorrectly() = runTest {
        repository.add(testUser)
        val updated = testUser.copy(name = "Alice Smith", password = "new_password")
        repository.update(updated)

        val loaded = repository.get(testUser.publicKey).first()
        assertEquals(updated, loaded)
    }

    @Test
    fun testUpdateName() = runTest {
        repository.add(testUser)
        repository.updateName(testUser.publicKey, "Bob")
        val loaded = repository.get(testUser.publicKey).first()
        assertEquals("Bob", loaded?.name)
    }

    @Test
    fun testUpdateStatusMessage() = runTest {
        repository.add(testUser)
        repository.updateStatusMessage(testUser.publicKey, "Busy working")
        val loaded = repository.get(testUser.publicKey).first()
        assertEquals("Busy working", loaded?.statusMessage)
    }

    @Test
    fun testUpdateConnection() = runTest {
        repository.add(testUser)
        repository.updateConnection(testUser.publicKey, ConnectionStatus.TCP)
        val loaded = repository.get(testUser.publicKey).first()
        assertEquals(ConnectionStatus.TCP, loaded?.connectionStatus)
    }

    @Test
    fun testUpdateStatus() = runTest {
        repository.add(testUser)
        repository.updateStatus(testUser.publicKey, UserStatus.Busy)
        val loaded = repository.get(testUser.publicKey).first()
        assertEquals(UserStatus.Busy, loaded?.status)
    }
}
