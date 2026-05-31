package ltd.evilcorp.core.platform.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.dao.FakeContactDao
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsBackupHelperImplTest {

    private lateinit var contactDao: FakeContactDao
    private lateinit var backupHelper: ContactsBackupHelperImpl

    @BeforeTest
    fun setUp() {
        contactDao = FakeContactDao()
        backupHelper = ContactsBackupHelperImpl(contactDao)
    }

    @Test
    fun testSerializeContacts_returnsAllDomainContacts() = runTest {
        val contact1 = ContactEntity("123", "Alice", "Busy", 100L, UserStatus.Busy, ConnectionStatus.UDP, false, "", false, "", 200L)
        val contact2 = ContactEntity("456", "Bob", "Away", 300L, UserStatus.Away, ConnectionStatus.TCP, true, "", true, "", 400L)
        contactDao.save(contact1)
        contactDao.save(contact2)

        val serialized = backupHelper.serializeContacts()
        assertEquals(2, serialized.size)

        val alice = serialized.first { it.publicKey == "123" }
        assertEquals("Alice", alice.name)
        assertEquals("Busy", alice.statusMessage)
        assertEquals(UserStatus.Busy, alice.status)
        assertEquals(ConnectionStatus.UDP, alice.connectionStatus)
    }

    @Test
    fun testDeserializeContacts_savesToDatabase() = runTest {
        val domainContact = Contact("789", "Charlie", "Hi", 500L, UserStatus.None, ConnectionStatus.None, false, "", false, "", 600L)
        backupHelper.deserializeContacts(listOf(domainContact))

        val loaded = contactDao.loadAllBlocking()
        assertEquals(1, loaded.size)
        assertEquals("Charlie", loaded[0].name)
        assertEquals("789", loaded[0].publicKey)
    }
}
