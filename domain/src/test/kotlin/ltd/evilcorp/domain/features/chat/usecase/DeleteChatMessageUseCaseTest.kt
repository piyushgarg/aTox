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
import ltd.evilcorp.domain.fakes.FakeContactRepository
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import ltd.evilcorp.domain.fakes.FakeTox
import kotlin.test.Test
import kotlin.test.assertTrue

class DeleteChatMessageUseCaseTest {

    @Test
    fun `execute deletes message by id`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val fakeTox = FakeTox()

        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val msg = Message(pk.string(), "Hello", Sender.Sent, MessageType.Normal, correlationId = 0, id = 99L)
        messageRepo.add(msg)

        val chatManager = ChatManager(scope, contactRepo, messageRepo, fakeTox, Dispatchers.Unconfined)
        val useCase = DeleteChatMessageUseCase(chatManager)

        // Act
        useCase.execute(99L)

        // Assert
        assertTrue(messageRepo.get(pk.string()).first().isEmpty())
    }
}
