package ltd.evilcorp.domain.features.chat.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.fakes.FakeContactRepository
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import ltd.evilcorp.domain.fakes.FakeTox
import kotlin.test.Test
import kotlin.test.assertEquals

class SendChatMessageUseCaseTest {

    @Test
    fun `execute sends simple message`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val fakeTox = FakeTox()

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        contactRepo.add(Contact(pk.string(), connectionStatus = ConnectionStatus.UDP))

        val chatManager = ChatManager(scope, contactRepo, messageRepo, fakeTox, Dispatchers.Unconfined)
        val useCase = SendChatMessageUseCase(chatManager, Dispatchers.Unconfined)

        // Act
        useCase.execute(pk, "Simple message", MessageType.Normal)

        // Assert
        assertEquals(1, fakeTox.sentMessages.size)
        assertEquals("Simple message", fakeTox.sentMessages[0].second)
    }

    @Test
    fun `execute sends reply message with formatted prefix`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val fakeTox = FakeTox()

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        contactRepo.add(Contact(pk.string(), connectionStatus = ConnectionStatus.UDP))

        val chatManager = ChatManager(scope, contactRepo, messageRepo, fakeTox, Dispatchers.Unconfined)
        val useCase = SendChatMessageUseCase(chatManager, Dispatchers.Unconfined)

        // Act
        useCase.execute(pk, "Replying to you", MessageType.Normal, replyToMessageId = 42)

        // Assert
        assertEquals(1, fakeTox.sentMessages.size)
        assertEquals("[reply:42] Replying to you", fakeTox.sentMessages[0].second)
    }

    @Test
    fun `execute chunks long messages exceeding MAX_MESSAGE_LENGTH`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val fakeTox = FakeTox()

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        contactRepo.add(Contact(pk.string(), connectionStatus = ConnectionStatus.UDP))

        val chatManager = ChatManager(scope, contactRepo, messageRepo, fakeTox, Dispatchers.Unconfined)
        val useCase = SendChatMessageUseCase(chatManager, Dispatchers.Unconfined)

        // 3000 characters (exceeds 1372 maximum length twice)
        val longMessage = "A".repeat(3000)

        // Act
        useCase.execute(pk, longMessage, MessageType.Normal)

        // Assert
        // 3000 / 1371 = 2 chunks of 1371 + 1 chunk of 258. Total 3 chunks sent.
        assertEquals(3, fakeTox.sentMessages.size)
        assertEquals("A".repeat(1371), fakeTox.sentMessages[0].second)
        assertEquals("A".repeat(1371), fakeTox.sentMessages[1].second)
        assertEquals("A".repeat(258), fakeTox.sentMessages[2].second)
    }
}
