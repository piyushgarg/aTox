// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.settings.usecase

import ltd.evilcorp.domain.core.network.ITox
import javax.inject.Inject

/**
 * Use case to retrieve the active startup/running status of the Tox backend network loop.
 */
class GetToxRunningStateUseCase @Inject constructor(
    private val tox: ITox,
) {
    fun execute(): Boolean {
        return tox.started
    }
}
