// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.settings.usecase

import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.IToxStarter
import javax.inject.Inject

sealed interface ToxLifecycleAction {
    object Stop : ToxLifecycleAction
    data class TryLoad(val password: String?) : ToxLifecycleAction
}

/**
 * Use case to manage core Tox connection startup/shutdown lifecycle.
 */
class ManageToxLifecycleUseCase @Inject constructor(
    private val tox: ITox,
    private val toxStarter: IToxStarter,
) {
    val started: Boolean get() = tox.started
    val password: String? get() = tox.password

    fun execute(action: ToxLifecycleAction) {
        when (action) {
            is ToxLifecycleAction.Stop -> toxStarter.stopTox()
            is ToxLifecycleAction.TryLoad -> toxStarter.tryLoadTox(action.password)
        }
    }
}
