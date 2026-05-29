// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.GroupManager
import javax.inject.Inject

/**
 * Use case to declare the active group chat currently in focus,
 * triggering automatic unread messages counter resetting.
 */
class SetActiveGroupUseCase @Inject constructor(
    private val groupManager: GroupManager,
) {
    fun execute(chatId: String) {
        groupManager.activeGroup = chatId
    }
}
