// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import javax.inject.Inject

/**
 * Use case to retrieve the active peer members list stream in a specific group chat.
 */
class GetGroupPeersUseCase @Inject constructor(
    private val groupRepository: IGroupRepository,
) {
    fun execute(chatId: String): Flow<List<GroupPeer>> {
        return groupRepository.getPeers(chatId)
    }
}
