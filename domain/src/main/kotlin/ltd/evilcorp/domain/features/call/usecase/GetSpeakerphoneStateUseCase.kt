// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.call.usecase

import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.domain.features.call.CallManager
import javax.inject.Inject

/**
 * Use case to retrieve the current speakerphone audio routing state.
 */
class GetSpeakerphoneStateUseCase @Inject constructor(
    private val callManager: CallManager,
) {
    val speakerphoneOnState: StateFlow<Boolean> get() = callManager.speakerphoneOnState
}
