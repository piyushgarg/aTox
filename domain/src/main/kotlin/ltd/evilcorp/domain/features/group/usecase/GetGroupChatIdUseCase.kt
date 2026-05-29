// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.firstOrNull
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import javax.inject.Inject

sealed interface ChatIdQuery {
    data class ByGroupChatId(val groupChatId: String) : ChatIdQuery
    data class ByGroupNumber(val groupNumber: Int) : ChatIdQuery
}

/**
 * Use case to query the hex string representation of a group's unique Chat ID.
 */
class GetGroupChatIdUseCase @Inject constructor(
    private val groupRepository: IGroupRepository,
) {
    suspend fun execute(query: ChatIdQuery): String? {
        return when (query) {
            is ChatIdQuery.ByGroupChatId -> groupRepository.get(query.groupChatId).firstOrNull()?.chatId
            is ChatIdQuery.ByGroupNumber -> groupRepository.findChatIdByGroupNumber(query.groupNumber)
        }
    }
}
