package ltd.evilcorp.core.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.entity.MessageEntity
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
class MessageDaoTest {

    private lateinit var db: Database
    private lateinit var dao: ltd.evilcorp.core.db.dao.MessageDao

    private val testMessage = MessageEntity(
        publicKey = "conversation123",
        message = "Hello World!",
        sender = Sender.Sent,
        type = MessageType.Normal,
        correlationId = 1234,
        timestamp = 0L
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, Database::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.messageDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSave_and_load() = runTest {
        dao.save(testMessage)
        val loaded = dao.load("conversation123").first()
        assertEquals(1, loaded.size)
        
        val loadedMsg = loaded[0]
        assertEquals("conversation123", loadedMsg.publicKey)
        assertEquals("Hello World!", loadedMsg.message)
        assertEquals(Sender.Sent, loadedMsg.sender)
        assertEquals(MessageType.Normal, loadedMsg.type)
        assertEquals(1234, loadedMsg.correlationId)
        assertEquals(0L, loadedMsg.timestamp)
    }

    @Test
    fun testSaveAll_and_loadAllBlocking() = runTest {
        val m1 = testMessage.copy(message = "One")
        val m2 = testMessage.copy(message = "Two")
        dao.saveAll(listOf(m1, m2))

        val loaded = dao.loadAll()
        assertEquals(2, loaded.size)
        assertTrue(loaded.any { it.message == "One" })
        assertTrue(loaded.any { it.message == "Two" })
    }

    @Test
    fun testLoadPending() = runTest {
        val m1 = testMessage.copy(message = "Pending1", timestamp = 0L)
        val m2 = testMessage.copy(message = "Sent1", timestamp = 5000L)
        dao.saveAll(listOf(m1, m2))

        val pending = dao.loadPending("conversation123")
        assertEquals(1, pending.size)
        assertEquals("Pending1", pending[0].message)
    }

    @Test
    fun testSetCorrelationId() = runTest {
        dao.save(testMessage)
        val all = dao.loadAll()
        val savedId = all[0].id

        dao.setCorrelationId(savedId, 9999)
        val updated = dao.loadAll()[0]
        assertEquals(9999, updated.correlationId)
    }

    @Test
    fun testDelete() = runTest {
        val m1 = testMessage.copy(publicKey = "chatA")
        val m2 = testMessage.copy(publicKey = "chatB")
        dao.saveAll(listOf(m1, m2))

        assertEquals(2, dao.loadAll().size)

        dao.delete("chatA")
        val remaining = dao.loadAll()
        assertEquals(1, remaining.size)
        assertEquals("chatB", remaining[0].publicKey)
    }

    @Test
    fun testDeleteMessage() = runTest {
        dao.save(testMessage)
        val savedId = dao.loadAll()[0].id

        dao.deleteMessage(savedId)
        assertTrue(dao.loadAll().isEmpty())
    }

    @Test
    fun testExists() = runTest {
        assertFalse(dao.exists("conversation123", "Hello World!"))
        
        dao.save(testMessage)
        assertTrue(dao.exists("conversation123", "Hello World!"))
    }

    @Test
    fun testSetReceipt() = runTest {
        dao.save(testMessage)
        assertEquals(0L, dao.loadAll()[0].timestamp)

        dao.setReceipt("conversation123", 1234, 9999L)
        assertEquals(9999L, dao.loadAll()[0].timestamp)
    }

    @Test
    fun testLoad_limitsAndOrders() = runTest {
        // Prepare 160 messages
        val list = (1..160).map { i ->
            testMessage.copy(message = "Msg $i")
        }
        dao.saveAll(list)

        val loaded = dao.load("conversation123").first()
        // Limit is 150
        assertEquals(150, loaded.size)
        // Order must be ASC of id, meaning from "Msg 11" to "Msg 160"
        assertEquals("Msg 11", loaded.first().message)
        assertEquals("Msg 160", loaded.last().message)
    }
}
