// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.call.usecase

import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.domain.features.call.CallManager
import ltd.evilcorp.domain.features.call.CallState
import javax.inject.Inject

/**
 * Use case to retrieve the active call state flow from the voice/audio network controller.
 */
class GetCallStateUseCase @Inject constructor(
    private val callManager: CallManager,
) {
    val inCall: StateFlow<CallState> get() = callManager.inCall
}
