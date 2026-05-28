// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.call.usecase

import javax.inject.Inject
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository

enum class CallHistoryType {
    Outgoing,
    Incoming,
    Missed,
    Cancelled,
}

class LogCallUseCase @Inject constructor(
    private val messageRepository: IMessageRepository,
) {
    suspend fun execute(publicKey: String, type: CallHistoryType) {
        val tag = when (type) {
            CallHistoryType.Outgoing -> "[CALL_HISTORY_OUTGOING]"
            CallHistoryType.Incoming -> "[CALL_HISTORY_INCOMING]"
            CallHistoryType.Missed -> "[CALL_HISTORY_MISSED]"
            CallHistoryType.Cancelled -> "[CALL_HISTORY_CANCELLED]"
        }
        val sender = when (type) {
            CallHistoryType.Outgoing, CallHistoryType.Cancelled -> Sender.Sent
            CallHistoryType.Incoming, CallHistoryType.Missed -> Sender.Received
        }
        messageRepository.add(
            Message(
                publicKey = publicKey,
                message = tag,
                sender = sender,
                type = MessageType.Action,
                correlationId = Int.MIN_VALUE,
            ),
        )
    }
}
