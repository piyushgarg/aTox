package ltd.evilcorp.domain.features.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.fakes.FakeContactRepository
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import ltd.evilcorp.domain.fakes.FakeTox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatManagerTest {

    @Test
    fun `sendMessage when contact is online sends directly via JNI and saves`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val fakeTox = FakeTox()

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val contact = Contact(pk.string(), connectionStatus = ConnectionStatus.UDP)
        contactRepo.add(contact)

        val manager = ChatManager(scope, contactRepo, messageRepo, fakeTox, Dispatchers.Unconfined)

        // Act
        manager.sendMessage(pk, "Hello Alice!", MessageType.Normal)

        // Assert
        // 1. Verify JNI call
        assertEquals(1, fakeTox.sentMessages.size)
        assertEquals(pk, fakeTox.sentMessages[0].first)
        assertEquals("Hello Alice!", fakeTox.sentMessages[0].second)

        // 2. Verify MessageRepo saved
        val msgs = messageRepo.get(pk.string()).first()
        assertEquals(1, msgs.size)
        assertEquals("Hello Alice!", msgs[0].message)
        assertEquals(1, msgs[0].correlationId) // returns 1 from FakeTox
    }

    @Test
    fun `sendMessage when contact is offline queues the message`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val fakeTox = FakeTox()

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val contact = Contact(pk.string(), connectionStatus = ConnectionStatus.None)
        contactRepo.add(contact)

        val manager = ChatManager(scope, contactRepo, messageRepo, fakeTox, Dispatchers.Unconfined)

        // Act
        manager.sendMessage(pk, "Offline message", MessageType.Normal)

        // Assert
        // JNI must not be called
        assertEquals(0, fakeTox.sentMessages.size)

        // MessageRepo must have the message queued (correlationId = Int.MIN_VALUE)
        val msgs = messageRepo.get(pk.string()).first()
        assertEquals(1, msgs.size)
        assertEquals("Offline message", msgs[0].message)
        assertEquals(Int.MIN_VALUE, msgs[0].correlationId)
    }

    @Test
    fun `clearHistory deletes messages and resets last message time`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val fakeTox = FakeTox()

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val contact = Contact(pk.string(), lastMessage = 55555L)
        contactRepo.add(contact)
        val manager = ChatManager(scope, contactRepo, messageRepo, fakeTox, Dispatchers.Unconfined)

        // Act
        manager.clearHistory(pk)

        // Assert
        val updatedContact = contactRepo.get(pk.string()).first()
        assertEquals(0L, updatedContact?.lastMessage)
        assertTrue(messageRepo.get(pk.string()).first().isEmpty())
    }

    @Test
    fun `resend sends messages in chronological order`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val fakeTox = FakeTox()

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val manager = ChatManager(scope, contactRepo, messageRepo, fakeTox, Dispatchers.Unconfined)

        val m1 = ltd.evilcorp.domain.features.chat.model.Message(
            publicKey = pk.string(),
            message = "First message",
            sender = ltd.evilcorp.domain.features.chat.model.Sender.Sent,
            type = MessageType.Normal,
            correlationId = Int.MIN_VALUE,
            id = 1L
        )
        val m2 = ltd.evilcorp.domain.features.chat.model.Message(
            publicKey = pk.string(),
            message = "Second message",
            sender = ltd.evilcorp.domain.features.chat.model.Sender.Sent,
            type = MessageType.Normal,
            correlationId = Int.MIN_VALUE,
            id = 2L
        )

        // Act
        manager.resend(listOf(m1, m2))

        // Assert
        assertEquals(2, fakeTox.sentMessages.size)
        assertEquals("First message", fakeTox.sentMessages[0].second)
        assertEquals("Second message", fakeTox.sentMessages[1].second)
    }
}
