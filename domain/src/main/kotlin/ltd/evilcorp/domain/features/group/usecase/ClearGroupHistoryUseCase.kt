// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import javax.inject.Inject

/**
 * Use case to clear all message history from a specific group chat.
 */
class ClearGroupHistoryUseCase @Inject constructor(
    private val groupRepository: IGroupRepository,
) {
    suspend fun execute(chatId: String) {
        groupRepository.deleteMessages(chatId)
        groupRepository.setLastMessage(chatId, 0L)
    }
}
