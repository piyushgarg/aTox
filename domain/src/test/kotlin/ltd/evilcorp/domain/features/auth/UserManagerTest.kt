package ltd.evilcorp.domain.features.auth

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.fakes.FakeUserRepository
import ltd.evilcorp.domain.fakes.FakeToxProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class UserManagerTest {

    @Test
    fun `get retrieves user from repository`() = runTest {
        val userRepo = FakeUserRepository()
        val toxProfile = FakeToxProfile()
        val userManager = UserManager(userRepo, toxProfile)

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val user = User(pk.string(), "Alice", "Hello", UserStatus.Away, ConnectionStatus.UDP)
        userRepo.add(user)

        val retrieved = userManager.get(pk).first()
        assertEquals(user, retrieved)
    }

    @Test
    fun `create user saves in repository and updates tox profile`() = runTest {
        val userRepo = FakeUserRepository()
        val toxProfile = FakeToxProfile()
        val userManager = UserManager(userRepo, toxProfile)

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val user = User(pk.string(), "Alice", "Hello", UserStatus.Away, ConnectionStatus.UDP)

        val result = userManager.create(user)
        assertTrue(result.isSuccess)

        val retrieved = userRepo.get(pk.string()).first()
        assertEquals(user, retrieved)
        assertEquals("Alice", toxProfile.getName())
        assertEquals("Hello", toxProfile.getStatusMessage())
    }

    @Test
    fun `create user updates existing record if user already exists`() = runTest {
        val userRepo = FakeUserRepository()
        val toxProfile = FakeToxProfile()
        val userManager = UserManager(userRepo, toxProfile)

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val initialUser = User(pk.string(), "Old Name", "Old Status")
        userRepo.add(initialUser)

        val newUser = User(pk.string(), "Alice", "Hello", UserStatus.Away, ConnectionStatus.UDP)
        val result = userManager.create(newUser)
        assertTrue(result.isSuccess)

        val retrieved = userRepo.get(pk.string()).first()
        assertEquals(newUser, retrieved)
        assertEquals("Alice", toxProfile.getName())
        assertEquals("Hello", toxProfile.getStatusMessage())
    }

    @Test
    fun `verifyExists creates user from tox profile if not exists`() = runTest {
        val userRepo = FakeUserRepository()
        val toxProfile = FakeToxProfile().apply {
            setName("Bob")
            setStatusMessage("Tox active")
        }
        val userManager = UserManager(userRepo, toxProfile)

        val pk = toxProfile.publicKey

        assertFalse(userRepo.exists(pk.string()))

        val result = userManager.verifyExists(pk)
        assertTrue(result.isSuccess)

        assertTrue(userRepo.exists(pk.string()))
        val retrieved = userRepo.get(pk.string()).first()
        assertEquals("Bob", retrieved?.name)
        assertEquals("Tox active", retrieved?.statusMessage)
    }

    @Test
    fun `verifyExists uses default values if tox profile name and status are empty`() = runTest {
        val userRepo = FakeUserRepository()
        val toxProfile = FakeToxProfile().apply {
            setName("")
            setStatusMessage("")
        }
        val userManager = UserManager(userRepo, toxProfile)

        val pk = toxProfile.publicKey
        val result = userManager.verifyExists(pk)
        assertTrue(result.isSuccess)

        val retrieved = userRepo.get(pk.string()).first()
        assertEquals("aTox user", retrieved?.name)
        assertEquals("Brought to you live, by aTox", retrieved?.statusMessage)
    }

    @Test
    fun `verifyExists does nothing if user already exists`() = runTest {
        val userRepo = FakeUserRepository()
        val toxProfile = FakeToxProfile()
        val userManager = UserManager(userRepo, toxProfile)

        val pk = toxProfile.publicKey
        val existingUser = User(pk.string(), "Alice", "Existing")
        userRepo.add(existingUser)

        val result = userManager.verifyExists(pk)
        assertTrue(result.isSuccess)

        val retrieved = userRepo.get(pk.string()).first()
        assertEquals("Alice", retrieved?.name)
    }

    @Test
    fun `setName updates both tox profile and repository`() = runTest {
        val userRepo = FakeUserRepository()
        val toxProfile = FakeToxProfile()
        val userManager = UserManager(userRepo, toxProfile)

        val pk = toxProfile.publicKey
        userRepo.add(User(pk.string(), "Alice", ""))

        val result = userManager.setName("Alice New")
        assertTrue(result.isSuccess)

        assertEquals("Alice New", toxProfile.getName())
        val retrieved = userRepo.get(pk.string()).first()
        assertEquals("Alice New", retrieved?.name)
    }

    @Test
    fun `setStatusMessage updates both tox profile and repository`() = runTest {
        val userRepo = FakeUserRepository()
        val toxProfile = FakeToxProfile()
        val userManager = UserManager(userRepo, toxProfile)

        val pk = toxProfile.publicKey
        userRepo.add(User(pk.string(), "", "Old status"))

        val result = userManager.setStatusMessage("New status")
        assertTrue(result.isSuccess)

        assertEquals("New status", toxProfile.getStatusMessage())
        val retrieved = userRepo.get(pk.string()).first()
        assertEquals("New status", retrieved?.statusMessage)
    }

    @Test
    fun `setStatus updates both tox profile and repository`() = runTest {
        val userRepo = FakeUserRepository()
        val toxProfile = FakeToxProfile()
        val userManager = UserManager(userRepo, toxProfile)

        val pk = toxProfile.publicKey
        userRepo.add(User(pk.string(), "", "", UserStatus.None))

        val result = userManager.setStatus(UserStatus.Busy)
        assertTrue(result.isSuccess)

        assertEquals(UserStatus.Busy, toxProfile.getStatus())
        val retrieved = userRepo.get(pk.string()).first()
        assertEquals(UserStatus.Busy, retrieved?.status)
    }
}
