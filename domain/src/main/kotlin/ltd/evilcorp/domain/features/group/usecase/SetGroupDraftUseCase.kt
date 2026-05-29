// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import javax.inject.Inject

/**
 * Use case to persist a message draft for a specific group chat.
 */
class SetGroupDraftUseCase @Inject constructor(
    private val groupRepository: IGroupRepository,
) {
    suspend fun execute(chatId: String, draft: String) {
        groupRepository.setDraftMessage(chatId, draft)
    }
}
