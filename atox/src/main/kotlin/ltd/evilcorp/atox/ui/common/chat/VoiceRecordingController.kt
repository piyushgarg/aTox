// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

private const val TAG = "VoiceRecordingController"
private const val SAMPLING_RATE = 44100
private const val BIT_RATE = 96000

/**
 * Controller encapsulating the lifecycle and actions of audio recording.
 * Isolates MediaRecorder native resource management from Composable layouts.
 */
class VoiceRecordingController(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var voiceFile: File? = null
    private var isRecordingActive = false

    fun startRecording(): Boolean {
        if (isRecordingActive) return false
        val file = File(context.cacheDir, "voice_message_${System.currentTimeMillis()}.m4a")
        voiceFile = file
        return try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLING_RATE)
                setAudioEncodingBitRate(BIT_RATE)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecordingActive = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            voiceFile?.delete()
            voiceFile = null
            isRecordingActive = false
            false
        }
    }

    fun stopRecording(): File? {
        if (!isRecordingActive) return null
        val file = voiceFile
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop mediaRecorder", e)
        }
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release mediaRecorder", e)
        }
        mediaRecorder = null
        voiceFile = null
        isRecordingActive = false
        return file
    }

    fun cancelRecording() {
        if (!isRecordingActive) return
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop mediaRecorder on cancel", e)
        }
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release mediaRecorder on cancel", e)
        }
        mediaRecorder = null
        voiceFile?.delete()
        voiceFile = null
        isRecordingActive = false
    }

    fun release() {
        cancelRecording()
    }
}
