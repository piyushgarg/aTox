package ltd.evilcorp.domain.features.call.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class LogCallUseCaseTest {

    @Test
    fun `execute logs outgoing call successfully`() = runTest {
        // Arrange
        val messageRepo = FakeMessageRepository()
        val useCase = LogCallUseCase(messageRepo)
        val pk = "3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C"

        // Act
        useCase.execute(pk, CallHistoryType.Outgoing)

        // Assert
        val msgs = messageRepo.get(pk).first()
        assertEquals(1, msgs.size)
        assertEquals("[CALL_HISTORY_OUTGOING]", msgs[0].message)
        assertEquals(Sender.Sent, msgs[0].sender)
        assertEquals(MessageType.Action, msgs[0].type)
        assertEquals(Int.MIN_VALUE, msgs[0].correlationId)
    }

    @Test
    fun `execute logs missed call successfully`() = runTest {
        // Arrange
        val messageRepo = FakeMessageRepository()
        val useCase = LogCallUseCase(messageRepo)
        val pk = "3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C"

        // Act
        useCase.execute(pk, CallHistoryType.Missed)

        // Assert
        val msgs = messageRepo.get(pk).first()
        assertEquals(1, msgs.size)
        assertEquals("[CALL_HISTORY_MISSED]", msgs[0].message)
        assertEquals(Sender.Received, msgs[0].sender)
    }
}
