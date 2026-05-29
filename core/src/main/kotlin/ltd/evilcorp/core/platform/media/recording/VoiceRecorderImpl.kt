// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.media.recording

import android.content.Context
import android.util.Log
import ltd.evilcorp.core.tox.OpusEncoder
import ltd.evilcorp.domain.features.call.service.IVoiceRecorder
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VoiceRecorderImpl"
private const val SAMPLING_RATE = 48000
private const val CHANNELS = 1
private const val FRAME_LENGTH_MS = 20
private const val FRAME_SIZE = 960 // 48000 * 20 / 1000
private const val JOIN_TIMEOUT_MS = 2000L

/**
 * Concrete implementation of [IVoiceRecorder].
 * Captures raw PCM shorts from the microphone and compresses them on the fly
 * to an Ogg Opus file (.opus) using JNI libopus.
 */
@Singleton
class VoiceRecorderImpl @Inject constructor(
    private val context: Context
) : IVoiceRecorder {
    private var voiceFile: File? = null
    @Volatile
    private var isRecordingActive = false
    private var recordingThread: Thread? = null
    private var audioCapture: AudioCapture? = null
    private var opusEncoder: OpusEncoder? = null
    private var encoderPtr: Long = 0

    @Synchronized
    override fun startRecording(): Boolean {
        if (isRecordingActive) return false
        val file = File(context.cacheDir, "voice_message_${System.currentTimeMillis()}.opus")
        voiceFile = file

        val capture = AudioCapture.create(
            sampleRate = SAMPLING_RATE,
            channels = CHANNELS,
            frameLengthMs = FRAME_LENGTH_MS,
            enableAutomaticGainControl = true
        )
        if (capture == null) {
            Log.e(TAG, "Failed to create AudioCapture")
            voiceFile = null
            return false
        }
        audioCapture = capture

        val encoder = OpusEncoder()
        val ptr = encoder.nativeCreate(SAMPLING_RATE, CHANNELS)
        if (ptr == 0L) {
            Log.e(TAG, "Failed to create native OpusEncoder")
            capture.release()
            audioCapture = null
            voiceFile = null
            return false
        }
        opusEncoder = encoder
        encoderPtr = ptr

        isRecordingActive = true
        startRecordingThread(file, capture, encoder, ptr)
        return true
    }

    private fun startRecordingThread(
        file: File,
        capture: AudioCapture,
        encoder: OpusEncoder,
        ptr: Long
    ) {
        val thread = Thread({
            var writer: OggOpusWriter? = null
            try {
                writer = OggOpusWriter(FileOutputStream(file))
                writer.writeHeader()

                capture.start()

                while (isRecordingActive) {
                    val pcmFrame = capture.read()
                    if (pcmFrame != null && isRecordingActive) {
                        val encoded = encoder.nativeEncode(ptr, pcmFrame, FRAME_SIZE)
                        if (encoded != null && encoded.isNotEmpty()) {
                            writer.writePacket(encoded, FRAME_SIZE)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during audio recording/encoding loop", e)
            } finally {
                cleanupCaptureAndEncoder(capture, writer, encoder, ptr)
            }
        }, "VoiceRecordingThread")

        recordingThread = thread
        thread.start()
    }

    private fun cleanupCaptureAndEncoder(
        capture: AudioCapture,
        writer: OggOpusWriter?,
        encoder: OpusEncoder,
        ptr: Long
    ) {
        try {
            capture.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop AudioCapture", e)
        }
        capture.release()

        try {
            writer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close OggOpusWriter", e)
        }

        try {
            encoder.nativeDestroy(ptr)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy native OpusEncoder", e)
        }
    }

    override suspend fun stopRecording(): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!isRecordingActive) return@withContext null
        isRecordingActive = false
        val file = voiceFile
        try {
            recordingThread?.join(JOIN_TIMEOUT_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join recording thread", e)
        }
        recordingThread = null
        audioCapture = null
        opusEncoder = null
        encoderPtr = 0
        voiceFile = null
        file?.absolutePath
    }

    override suspend fun cancelRecording() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!isRecordingActive) return@withContext
        isRecordingActive = false
        val file = voiceFile
        try {
            recordingThread?.join(JOIN_TIMEOUT_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join recording thread on cancel", e)
        }
        recordingThread = null
        audioCapture = null
        opusEncoder = null
        encoderPtr = 0
        file?.delete()
        voiceFile = null
    }

    override fun release() {
        isRecordingActive = false
    }
}
