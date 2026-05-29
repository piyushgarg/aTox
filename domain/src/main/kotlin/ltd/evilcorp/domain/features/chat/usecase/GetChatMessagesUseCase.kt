// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.chat.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.core.model.PublicKey
import javax.inject.Inject

/**
 * Use case to retrieve the active message history stream for a specific contact chat.
 */
class GetChatMessagesUseCase @Inject constructor(
    private val chatManager: ChatManager,
) {
    fun execute(publicKey: PublicKey): Flow<List<Message>> {
        return chatManager.messagesFor(publicKey)
    }
}
