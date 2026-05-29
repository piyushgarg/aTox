// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.chat.usecase

import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.core.model.PublicKey
import javax.inject.Inject

/**
 * Use case to update the currently open/active private contact chat.
 */
class SetActiveChatUseCase @Inject constructor(
    private val chatManager: ChatManager,
) {
    fun execute(publicKey: PublicKey) {
        chatManager.activeChat = publicKey.string()
    }
}
