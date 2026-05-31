package ltd.evilcorp.core.platform.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.dao.FakeMessageDao
import ltd.evilcorp.core.db.entity.MessageEntity
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatHistoryBackupHelperImplTest {

    private lateinit var messageDao: FakeMessageDao
    private lateinit var backupHelper: ChatHistoryBackupHelperImpl

    @BeforeTest
    fun setUp() {
        messageDao = FakeMessageDao()
        backupHelper = ChatHistoryBackupHelperImpl(messageDao)
    }

    @Test
    fun testSerializeChatHistory_returnsAllDomainMessages() = runTest {
        val msg1 = MessageEntity("user1", "Hello", Sender.Sent, MessageType.Normal, 1, 1000L).apply { id = 10L }
        val msg2 = MessageEntity("user2", "Hi", Sender.Received, MessageType.Normal, 2, 2000L).apply { id = 11L }
        messageDao.saveAll(listOf(msg1, msg2))

        val serialized = backupHelper.serializeChatHistory()
        assertEquals(2, serialized.size)
        
        val first = serialized.first { it.id == 10L }
        assertEquals("user1", first.publicKey)
        assertEquals("Hello", first.message)
        assertEquals(Sender.Sent, first.sender)
        assertEquals(MessageType.Normal, first.type)
        assertEquals(1, first.correlationId)
        assertEquals(1000L, first.timestamp)
    }

    @Test
    fun testDeserializeChatHistory_savesToDatabase() = runTest {
        val domainMsg = Message("user3", "Howdy", Sender.Sent, MessageType.Normal, 5, 3000L).apply { id = 20L }
        backupHelper.deserializeChatHistory(listOf(domainMsg))

        val loaded = messageDao.loadAllBlocking()
        assertEquals(1, loaded.size)
        assertEquals("user3", loaded[0].publicKey)
        assertEquals("Howdy", loaded[0].message)
        assertEquals(20L, loaded[0].id)
    }

    @Test
    fun testSerializeCallLog_returnsOnlyCallMessages() = runTest {
        // Call logs are messages where correlationId is Int.MIN_VALUE
        val chatMsg = MessageEntity("user1", "Chat message", Sender.Sent, MessageType.Normal, 1, 1000L).apply { id = 1L }
        val callMsg1 = MessageEntity("user2", "Call active", Sender.Received, MessageType.Normal, Int.MIN_VALUE, 2000L).apply { id = 2L }
        val callMsg2 = MessageEntity("user3", "Call ended", Sender.Sent, MessageType.Normal, Int.MIN_VALUE, 3000L).apply { id = 3L }
        
        messageDao.saveAll(listOf(chatMsg, callMsg1, callMsg2))

        val serialized = backupHelper.serializeCallLog()
        assertEquals(2, serialized.size)
        assertTrue(serialized.any { it.id == 2L })
        assertTrue(serialized.any { it.id == 3L })
        assertTrue(serialized.none { it.id == 1L })
    }

    @Test
    fun testDeserializeCallLog_savesToDatabase() = runTest {
        val callMsg = Message("user4", "Call log entry", Sender.Received, MessageType.Normal, Int.MIN_VALUE, 4000L).apply { id = 4L }
        backupHelper.deserializeCallLog(listOf(callMsg))

        val loaded = messageDao.loadAllBlocking()
        assertEquals(1, loaded.size)
        assertEquals("user4", loaded[0].publicKey)
        assertEquals(Int.MIN_VALUE, loaded[0].correlationId)
        assertEquals(4L, loaded[0].id)
    }
}
