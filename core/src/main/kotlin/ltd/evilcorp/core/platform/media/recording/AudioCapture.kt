// SPDX-FileCopyrightText: 2021 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.media.recording

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.media.MediaRecorder
import android.util.Log

private const val TAG = "AudioCapture"

private fun intToChannel(channels: Int) = when (channels) {
    1 -> AudioFormat.CHANNEL_IN_MONO
    else -> AudioFormat.CHANNEL_IN_STEREO
}

// The permission linting doesn't work very well unless you sprinkle
// ContextCompat.checkSelfPermission in way too many places. It doesn't even
// agree with results from ActivityResultContracts.RequestPermission, requiring
// an extra permission check in there as well.
@SuppressLint("MissingPermission")
private fun findAudioRecord(sampleRate: Int, channels: Int): AudioRecord? {
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val channelConfig = intToChannel(channels)

    val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
        return null
    }
    // Double buffer size to prevent overflows and jank under high CPU load
    val bufferSize = minBufferSize * 2

    // Seems like not all Xiaomi phones have a VOICE_COMMUNICATION audio source, so try a few different ones.
    val audioSources = arrayOf(
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.MIC,
        MediaRecorder.AudioSource.DEFAULT,
    )
    for (audioSource in audioSources) {
        val recorder = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.w(TAG, "Failed to initialize audio record $audioSource")
            continue
        }
        return recorder
    }

    return null
}

private const val MS_PER_SECOND = 1000.0

class AudioCapture private constructor(
    sampleRate: Int,
    channels: Int,
    frameLengthMs: Int,
    private val audioRecord: AudioRecord,
    private val effects: List<android.media.audiofx.AudioEffect>,
) {
    fun start() {
        runCatching { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO) }
        audioRecord.startRecording()
    }
    fun stop() = audioRecord.stop()
    fun release() {
        effects.forEach { effect ->
            runCatching { effect.release() }
        }
        audioRecord.release()
    }

    private val pcmBuffer = ShortArray((sampleRate * channels * frameLengthMs / MS_PER_SECOND).toInt())

    fun read(): ShortArray? {
        val read = audioRecord.read(pcmBuffer, 0, pcmBuffer.size)
        if (read <= 0) {
            Log.w(TAG, "AudioRecord read failed: $read")
            return null
        }
        if (read < pcmBuffer.size) {
            pcmBuffer.fill(0, fromIndex = read)
        }
        return pcmBuffer
    }

    companion object {
        fun create(
            sampleRate: Int,
            channels: Int,
            frameLengthMs: Int,
            enableAutomaticGainControl: Boolean = true,
        ): AudioCapture? {
            val audioRecord = findAudioRecord(sampleRate, channels) ?: return null
            val sessionId = audioRecord.audioSessionId
            val effects = listOfNotNull(
                createEffect("AcousticEchoCanceler", AcousticEchoCanceler.isAvailable()) {
                    AcousticEchoCanceler.create(sessionId)
                },
                createEffect("NoiseSuppressor", NoiseSuppressor.isAvailable()) {
                    NoiseSuppressor.create(sessionId)
                },
                if (enableAutomaticGainControl) {
                    createEffect("AutomaticGainControl", AutomaticGainControl.isAvailable()) {
                        AutomaticGainControl.create(sessionId)
                    }
                } else {
                    null
                },
            )
            return AudioCapture(sampleRate, channels, frameLengthMs, audioRecord, effects)
        }

        private fun createEffect(
            name: String,
            available: Boolean,
            factory: () -> android.media.audiofx.AudioEffect?,
        ): android.media.audiofx.AudioEffect? {
            if (!available) return null
            return runCatching {
                factory()?.apply { enabled = true }
            }.onFailure {
                Log.w(TAG, "Failed to enable $name", it)
            }.getOrNull()
        }
    }
}
