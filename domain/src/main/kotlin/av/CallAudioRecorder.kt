// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.av

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.domain.tox.ITox
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CallAudioRecorder"
private const val AUDIO_CHANNELS = 1
private const val AUDIO_SAMPLING_RATE_HZ = 48_000
private const val AUDIO_SEND_INTERVAL_MS = 20

@Singleton
class CallAudioRecorder @Inject constructor(
    private val tox: ITox
) {
    private val _sendingAudio = MutableStateFlow(false)
    val sendingAudio: StateFlow<Boolean> get() = _sendingAudio

    private var audioCaptureJob: Job? = null

    @Synchronized
    fun startAudioCapture(
        scope: CoroutineScope,
        to: PublicKey,
        speakerphoneOn: Boolean,
        isTargetValid: () -> Boolean
    ) {
        if (audioCaptureJob?.isActive == true) return
        val recorder = AudioCapture.create(
            AUDIO_SAMPLING_RATE_HZ,
            AUDIO_CHANNELS,
            AUDIO_SEND_INTERVAL_MS,
            enableAutomaticGainControl = !speakerphoneOn,
        ) ?: return

        _sendingAudio.value = true
        audioCaptureJob = scope.launch {
            recorder.start()
            try {
                while (_sendingAudio.value && isTargetValid()) {
                    val start = System.currentTimeMillis()
                    val audioFrame = recorder.read()
                    if (audioFrame != null) {
                        runCatching {
                            tox.sendAudio(to, audioFrame, AUDIO_CHANNELS, AUDIO_SAMPLING_RATE_HZ)
                        }
                    }
                    val elapsed = System.currentTimeMillis() - start
                    if (elapsed < AUDIO_SEND_INTERVAL_MS) {
                        delay(AUDIO_SEND_INTERVAL_MS - elapsed)
                    }
                }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
                _sendingAudio.value = false
            }
        }
    }

    @Synchronized
    fun stopAudioCapture() {
        _sendingAudio.value = false
        audioCaptureJob?.cancel()
        audioCaptureJob = null
    }

    @Synchronized
    fun isRecordingActive(): Boolean {
        return audioCaptureJob?.isActive == true
    }

    suspend fun joinRecording() {
        audioCaptureJob?.join()
    }
}
