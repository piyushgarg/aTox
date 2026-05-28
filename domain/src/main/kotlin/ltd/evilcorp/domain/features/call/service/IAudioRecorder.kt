// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.call.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.domain.core.model.PublicKey

interface IAudioRecorder {
    val sendingAudio: StateFlow<Boolean>

    fun startAudioCapture(
        scope: CoroutineScope,
        to: PublicKey,
        speakerphoneOn: Boolean,
        isTargetValid: () -> Boolean
    )

    fun stopAudioCapture()
    fun isRecordingActive(): Boolean
    suspend fun joinRecording()
}
