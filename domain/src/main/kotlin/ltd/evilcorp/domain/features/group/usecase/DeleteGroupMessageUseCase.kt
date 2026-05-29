// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import javax.inject.Inject

/**
 * Use case to delete a specific message from group chat database storage.
 */
class DeleteGroupMessageUseCase @Inject constructor(
    private val groupRepository: IGroupRepository,
) {
    suspend fun execute(messageId: Long) {
        groupRepository.deleteMessage(messageId)
    }
}
