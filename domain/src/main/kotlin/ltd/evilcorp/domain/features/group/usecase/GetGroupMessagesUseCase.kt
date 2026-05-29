// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import javax.inject.Inject

/**
 * Use case to query the stream of all active messages in a specific group chat.
 */
class GetGroupMessagesUseCase @Inject constructor(
    private val groupRepository: IGroupRepository,
) {
    fun execute(chatId: String): Flow<List<GroupMessage>> {
        return groupRepository.getMessages(chatId)
    }
}
