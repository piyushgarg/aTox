// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.GroupManager
import javax.inject.Inject

/**
 * Use case to retrieve the active stream list of all group chats.
 */
class GetGroupsUseCase @Inject constructor(
    private val groupManager: GroupManager,
) {
    fun execute(): Flow<List<Group>> {
        return groupManager.getAll()
    }
}
