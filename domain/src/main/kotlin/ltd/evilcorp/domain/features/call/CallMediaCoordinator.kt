// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.call

import javax.inject.Inject
import ltd.evilcorp.domain.features.call.service.ICallSignalPlayer
import ltd.evilcorp.domain.features.call.service.IAudioRecorder

/**
 * Coordinator grouping call audio, signal playing, and routing dependencies
 * to maintain Clean Architecture constructor parameters bounds.
 */
class CallMediaCoordinator @Inject constructor(
    val signalPlayer: ICallSignalPlayer,
    val audioRecorder: IAudioRecorder,
    val audioRoutingManager: IAudioRoutingManager,
)
