// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.chat.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import kotlin.test.Test
import kotlin.test.assertNotNull

class GetChatMessagesPagedUseCaseTest {

    @Test
    fun `execute returns paged message history flow`() = runTest {
        // Arrange
        val messageRepo = FakeMessageRepository()
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val msg = Message(pk.string(), "Hello Paged", Sender.Sent, MessageType.Normal, correlationId = 0)
        messageRepo.add(msg)

        val useCase = GetChatMessagesPagedUseCase(messageRepo)

        // Act
        val flow = useCase.execute(pk)
        val result = flow.first()

        // Assert
        assertNotNull(result)
    }
}
