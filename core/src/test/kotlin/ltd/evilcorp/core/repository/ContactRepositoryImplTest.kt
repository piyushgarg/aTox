package ltd.evilcorp.core.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.dao.FakeContactDao
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContactRepositoryImplTest {

    private lateinit var dao: FakeContactDao
    private lateinit var repository: ContactRepositoryImpl

    private val testContact = Contact(
        publicKey = "5678",
        name = "Alice",
        statusMessage = "Hello World",
        lastMessage = 1000L,
        status = UserStatus.None,
        connectionStatus = ConnectionStatus.None,
        typing = false,
        avatarUri = "content://avatar",
        hasUnreadMessages = false,
        draftMessage = "Draft",
        lastOnline = 2000L
    )

    @BeforeTest
    fun setUp() {
        dao = FakeContactDao()
        repository = ContactRepositoryImpl(dao)
    }

    @Test
    fun testExists_nonExistent_returnsFalse() = runTest {
        assertFalse(repository.exists(testContact.publicKey))
    }

    @Test
    fun testExists_existent_returnsTrue() = runTest {
        repository.add(testContact)
        assertTrue(repository.exists(testContact.publicKey))
    }

    @Test
    fun testAdd_and_get_returnsCorrectContact() = runTest {
        repository.add(testContact)
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals(testContact, loaded)
    }

    @Test
    fun testGet_nonExistent_returnsNull() = runTest {
        val loaded = repository.get("non-existent").first()
        assertNull(loaded)
    }

    @Test
    fun testUpdate_modifiesDatabaseAndCache() = runTest {
        repository.add(testContact)
        val updated = testContact.copy(name = "Alice Smith", connectionStatus = ConnectionStatus.TCP)
        repository.update(updated)
        
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals(updated, loaded)
    }

    @Test
    fun testDelete_removesFromDatabaseAndCache() = runTest {
        repository.add(testContact)
        assertTrue(repository.exists(testContact.publicKey))
        
        repository.delete(testContact)
        assertFalse(repository.exists(testContact.publicKey))
        assertNull(repository.get(testContact.publicKey).first())
    }

    @Test
    fun testGetAll_returnsAllContacts() = runTest {
        val secondContact = testContact.copy(publicKey = "9999", name = "Bob")
        repository.add(testContact)
        repository.add(secondContact)

        val all = repository.getAll().first()
        assertEquals(2, all.size)
        assertTrue(all.contains(testContact))
        assertTrue(all.contains(secondContact))
    }

    @Test
    fun testResetTransientData_clearsCacheAndResetsDaoTransientFields() = runTest {
        val contactWithTransient = testContact.copy(connectionStatus = ConnectionStatus.UDP, typing = true)
        repository.add(contactWithTransient)

        repository.resetTransientData()

        val loaded = repository.get(testContact.publicKey).first()
        assertEquals(ConnectionStatus.None, loaded?.connectionStatus)
        assertEquals(false, loaded?.typing)
    }

    @Test
    fun testSetName() = runTest {
        repository.add(testContact)
        repository.setName(testContact.publicKey, "Bob")
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals("Bob", loaded?.name)
    }

    @Test
    fun testSetStatusMessage() = runTest {
        repository.add(testContact)
        repository.setStatusMessage(testContact.publicKey, "Busy")
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals("Busy", loaded?.statusMessage)
    }

    @Test
    fun testSetLastMessage() = runTest {
        repository.add(testContact)
        repository.setLastMessage(testContact.publicKey, 9999L)
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals(9999L, loaded?.lastMessage)
    }

    @Test
    fun testSetUserStatus() = runTest {
        repository.add(testContact)
        repository.setUserStatus(testContact.publicKey, UserStatus.Busy)
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals(UserStatus.Busy, loaded?.status)
    }

    @Test
    fun testSetConnectionStatus() = runTest {
        repository.add(testContact)
        repository.setConnectionStatus(testContact.publicKey, ConnectionStatus.TCP)
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals(ConnectionStatus.TCP, loaded?.connectionStatus)
    }

    @Test
    fun testSetTyping() = runTest {
        repository.add(testContact)
        repository.setTyping(testContact.publicKey, true)
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals(true, loaded?.typing)
    }

    @Test
    fun testSetAvatarUri() = runTest {
        repository.add(testContact)
        repository.setAvatarUri(testContact.publicKey, "new-uri")
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals("new-uri", loaded?.avatarUri)
    }

    @Test
    fun testSetHasUnreadMessages() = runTest {
        repository.add(testContact)
        repository.setHasUnreadMessages(testContact.publicKey, true)
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals(true, loaded?.hasUnreadMessages)
    }

    @Test
    fun testSetDraftMessage() = runTest {
        repository.add(testContact)
        repository.setDraftMessage(testContact.publicKey, "new-draft")
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals("new-draft", loaded?.draftMessage)
    }

    @Test
    fun testSetLastOnline() = runTest {
        repository.add(testContact)
        repository.setLastOnline(testContact.publicKey, 12345L)
        val loaded = repository.get(testContact.publicKey).first()
        assertEquals(12345L, loaded?.lastOnline)
    }
}
