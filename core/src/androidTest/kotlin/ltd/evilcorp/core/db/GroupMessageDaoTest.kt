package ltd.evilcorp.core.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.entity.GroupMessageEntity
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
class GroupMessageDaoTest {

    private lateinit var db: Database
    private lateinit var dao: ltd.evilcorp.core.db.dao.GroupMessageDao

    private val testMessage = GroupMessageEntity(
        groupChatId = "group123",
        peerId = 5,
        senderName = "Charlie",
        message = "Hello group!",
        sender = Sender.Received,
        type = MessageType.Normal,
        correlationId = 101,
        timestamp = 0L
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, Database::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.groupMessageDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSave_and_load() = runTest {
        assertTrue(dao.load(testMessage.groupChatId).first().isEmpty())

        dao.save(testMessage)
        val loaded = dao.load(testMessage.groupChatId).first()
        assertEquals(1, loaded.size)
        
        val loadedMsg = loaded[0]
        assertEquals("group123", loadedMsg.groupChatId)
        assertEquals(5, loadedMsg.peerId)
        assertEquals("Charlie", loadedMsg.senderName)
        assertEquals("Hello group!", loadedMsg.message)
        assertEquals(Sender.Received, loadedMsg.sender)
        assertEquals(MessageType.Normal, loadedMsg.type)
        assertEquals(101, loadedMsg.correlationId)
        assertEquals(0L, loadedMsg.timestamp)
    }

    @Test
    fun testLoadPending() = runTest {
        val m1 = testMessage.copy(message = "Pending1", timestamp = 0L)
        val m2 = testMessage.copy(message = "Delivered1", timestamp = 5000L)
        dao.save(m1)
        dao.save(m2)

        val pending = dao.loadPending(testMessage.groupChatId)
        assertEquals(1, pending.size)
        assertEquals("Pending1", pending[0].message)
    }

    @Test
    fun testLoadUnsent() = runTest {
        // Unsent messages: correlationId == -1, sender == Sent (0), type != GroupEvent (3)
        val m1 = testMessage.copy(message = "Unsent", sender = Sender.Sent, correlationId = -1, type = MessageType.Normal)
        val m2 = testMessage.copy(message = "Sent", sender = Sender.Sent, correlationId = 105, type = MessageType.Normal)
        val m3 = testMessage.copy(message = "UnsentGroupEvent", sender = Sender.Sent, correlationId = -1, type = MessageType.GroupEvent)
        
        dao.save(m1)
        dao.save(m2)
        dao.save(m3)

        val unsent = dao.loadUnsent(testMessage.groupChatId)
        assertEquals(1, unsent.size)
        assertEquals("Unsent", unsent[0].message)
    }

    @Test
    fun testSetCorrelationId() = runTest {
        dao.save(testMessage)
        val saved = dao.load(testMessage.groupChatId).first()[0]

        dao.setCorrelationId(saved.id, 999)
        val updated = dao.load(testMessage.groupChatId).first()[0]
        assertEquals(999, updated.correlationId)
    }

    @Test
    fun testDelete() = runTest {
        dao.save(testMessage)
        dao.save(testMessage.copy(groupChatId = "group456"))

        dao.delete("group123")
        
        assertTrue(dao.load("group123").first().isEmpty())
        assertEquals(1, dao.load("group456").first().size)
    }

    @Test
    fun testSetReceipt() = runTest {
        dao.save(testMessage)
        val saved = dao.load(testMessage.groupChatId).first()[0]
        assertEquals(0L, saved.timestamp)

        dao.setReceipt(testMessage.groupChatId, 101, 8888L)
        val updated = dao.load(testMessage.groupChatId).first()[0]
        assertEquals(8888L, updated.timestamp)
    }

    @Test
    fun testExistsByCorrelationId() = runTest {
        assertEquals(0, dao.existsByCorrelationId(testMessage.groupChatId, 101))

        dao.save(testMessage)
        assertEquals(1, dao.existsByCorrelationId(testMessage.groupChatId, 101))
    }

    @Test
    fun testGetMessageIds() = runTest {
        val m1 = testMessage.copy(correlationId = 500)
        val m2 = testMessage.copy(correlationId = 600)
        dao.save(m1)
        dao.save(m2)

        val ids = dao.getMessageIds(testMessage.groupChatId)
        assertEquals(2, ids.size)
        assertTrue(ids.contains(500))
        assertTrue(ids.contains(600))
    }

    @Test
    fun testGetMessagesByIds() = runTest {
        val m1 = testMessage.copy(correlationId = 700)
        val m2 = testMessage.copy(correlationId = 800)
        val m3 = testMessage.copy(correlationId = 900)
        dao.save(m1)
        dao.save(m2)
        dao.save(m3)

        val selected = dao.getMessagesByIds(testMessage.groupChatId, setOf(700, 900))
        assertEquals(2, selected.size)
        assertTrue(selected.any { it.correlationId == 700 })
        assertTrue(selected.any { it.correlationId == 900 })
        assertFalse(selected.any { it.correlationId == 800 })
    }

    @Test
    fun testDeleteMessage() = runTest {
        dao.save(testMessage)
        val saved = dao.load(testMessage.groupChatId).first()[0]

        dao.deleteMessage(saved.id)
        assertTrue(dao.load(testMessage.groupChatId).first().isEmpty())
    }
}
