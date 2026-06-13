package ltd.evilcorp.domain.features.chat.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.fakes.FakeContactRepository
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import ltd.evilcorp.domain.fakes.FakeTox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClearChatHistoryUseCaseTest {

    @Test
    fun `execute clears message history and resets last message timestamp`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val fakeTox = FakeTox()

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        contactRepo.add(Contact(pk.string(), lastMessage = 99999L))
        messageRepo.add(Message(pk.string(), "Hello", Sender.Sent, MessageType.Normal, correlationId = 0))

        val chatManager = ChatManager(scope, contactRepo, messageRepo, fakeTox, Dispatchers.Unconfined)
        val useCase = ClearChatHistoryUseCase(chatManager)

        // Act
        useCase.execute(pk)

        // Assert
        val updatedContact = contactRepo.get(pk.string()).first()
        assertEquals(0L, updatedContact?.lastMessage)
        assertTrue(messageRepo.get(pk.string()).first().isEmpty())
    }
}
