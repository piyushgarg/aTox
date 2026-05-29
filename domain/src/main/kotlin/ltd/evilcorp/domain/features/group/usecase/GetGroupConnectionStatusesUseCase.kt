// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupManager
import javax.inject.Inject

/**
 * Use case to retrieve the active map stream of connection statuses for all group chats.
 */
class GetGroupConnectionStatusesUseCase @Inject constructor(
    private val groupManager: GroupManager,
) {
    fun execute(): Flow<Map<String, GroupConnectionStatus>> {
        return groupManager.connectionStatuses
    }
}
