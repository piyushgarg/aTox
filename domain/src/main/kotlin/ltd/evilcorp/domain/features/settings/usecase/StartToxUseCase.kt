// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.settings.usecase

import ltd.evilcorp.domain.core.network.IToxStarter
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import javax.inject.Inject

/**
 * Use case to startup the Tox backend loop with raw save data and password parameters.
 */
class StartToxUseCase @Inject constructor(
    private val toxStarter: IToxStarter,
) {
    fun execute(save: ByteArray?, password: String?): ToxSaveStatus {
        return toxStarter.startTox(save, password)
    }
}
