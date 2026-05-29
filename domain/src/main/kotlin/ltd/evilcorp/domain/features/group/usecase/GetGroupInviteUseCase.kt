// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.IGroupSessionRegistry
import javax.inject.Inject

/**
 * Use case to retrieve the active stream of pending group chat invites.
 */
class GetGroupInviteUseCase @Inject constructor(
    private val sessionRegistry: IGroupSessionRegistry,
) {
    fun execute(): Flow<GroupInvite?> {
        return sessionRegistry.pendingInvite
    }
}
