// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.GroupConnectionService
import javax.inject.Inject

/**
 * Use case to reconnect all disconnected group chats on start or connection state change.
 */
class ReconnectGroupsUseCase @Inject constructor(
    private val groupConnectionService: GroupConnectionService,
) {
    fun execute() {
        groupConnectionService.reconnectAll()
    }
}
