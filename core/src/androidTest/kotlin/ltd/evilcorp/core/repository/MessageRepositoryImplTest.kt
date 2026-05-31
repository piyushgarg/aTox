package ltd.evilcorp.core.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MessageRepositoryImplTest {

    private lateinit var db: Database
    private lateinit var repository: MessageRepositoryImpl

    private val testMessage = Message(
        publicKey = "contact_pub_key",
        message = "Hi Alice!",
        sender = Sender.Sent,
        type = MessageType.Normal,
        correlationId = 100,
        timestamp = 0L
    )

    @Before
    fun setUp() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, Database::class.java)
            .allowMainThreadQueries()
            .build()
        
        // Seed contact inside db so that updating last message doesn't crash on foreign keys if any (good practice)
        val contact = ContactEntity(publicKey = "contact_pub_key")
        db.contactDao().save(contact)

        repository = MessageRepositoryImpl(db, db.messageDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAdd_savesMessageAndUpdatesContactLastMessage() = runTest {
        val initialContact = db.contactDao().load("contact_pub_key").first()
        assertEquals(0L, initialContact?.lastMessage)

        repository.add(testMessage)

        // Verify message is saved
        val messages = repository.get("contact_pub_key").first()
        assertEquals(1, messages.size)
        assertEquals("Hi Alice!", messages[0].message)

        // Verify contact's lastMessage is updated to a recent time > 0
        val updatedContact = db.contactDao().load("contact_pub_key").first()
        assertTrue((updatedContact?.lastMessage ?: 0L) > 0L)
    }

    @Test
    fun testGetPending() = runTest {
        val m1 = testMessage.copy(message = "Pending 1", timestamp = 0L)
        val m2 = testMessage.copy(message = "Delivered 1", timestamp = 9999L)
        
        repository.add(m1)
        repository.add(m2)

        val pending = repository.getPending("contact_pub_key")
        assertEquals(1, pending.size)
        assertEquals("Pending 1", pending[0].message)
    }

    @Test
    fun testSetCorrelationId() = runTest {
        repository.add(testMessage)
        val saved = repository.get("contact_pub_key").first()[0]

        repository.setCorrelationId(saved.id, 999)
        val updated = repository.get("contact_pub_key").first()[0]
        assertEquals(999, updated.correlationId)
    }

    @Test
    fun testDelete() = runTest {
        repository.add(testMessage)
        assertEquals(1, repository.get("contact_pub_key").first().size)

        repository.delete("contact_pub_key")
        assertTrue(repository.get("contact_pub_key").first().isEmpty())
    }

    @Test
    fun testDeleteMessage() = runTest {
        repository.add(testMessage)
        val saved = repository.get("contact_pub_key").first()[0]

        repository.deleteMessage(saved.id)
        assertTrue(repository.get("contact_pub_key").first().isEmpty())
    }

    @Test
    fun testExists() = runTest {
        assertFalse(repository.exists("contact_pub_key", "Hi Alice!"))

        repository.add(testMessage)
        assertTrue(repository.exists("contact_pub_key", "Hi Alice!"))
    }

    @Test
    fun testSetReceipt() = runTest {
        repository.add(testMessage)
        val saved = repository.get("contact_pub_key").first()[0]
        assertEquals(0L, saved.timestamp)

        repository.setReceipt("contact_pub_key", 100, 7777L)
        val updated = repository.get("contact_pub_key").first()[0]
        assertEquals(7777L, updated.timestamp)
    }
}
