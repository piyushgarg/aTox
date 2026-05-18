// SPDX-FileCopyrightText: 2021-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.feature

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.FINGERPRINT_LEN
import ltd.evilcorp.domain.av.AudioCapture
import ltd.evilcorp.domain.tox.Tox

sealed class CallState {
    object NotInCall : CallState()
    data class InCall(val publicKey: PublicKey, val startTime: Long) : CallState()
}

private const val TAG = "CallManager"

private const val AUDIO_CHANNELS = 1
private const val AUDIO_SAMPLING_RATE_HZ = 48_000
private const val AUDIO_SEND_INTERVAL_MS = 20

@Singleton
class CallManager @Inject constructor(private val tox: Tox, private val scope: CoroutineScope, context: Context) {
    private val _inCall = MutableStateFlow<CallState>(CallState.NotInCall)
    val inCall: StateFlow<CallState> get() = _inCall

    private val _pendingCalls = MutableStateFlow<MutableSet<Contact>>(mutableSetOf())
    val pendingCalls: StateFlow<Set<Contact>> get() = _pendingCalls

    private val _sendingAudio = MutableStateFlow(false)
    val sendingAudio: StateFlow<Boolean> get() = _sendingAudio

    private val audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)

    fun addPendingCall(from: Contact) {
        val calls = mutableSetOf<Contact>().apply { addAll(_pendingCalls.value) }
        calls.addAll(_pendingCalls.value)
        if (calls.add(from)) {
            Log.i(TAG, "Added pending call ${from.publicKey.take(FINGERPRINT_LEN)}")
            _pendingCalls.value = calls
        }
    }

    fun removePendingCall(pk: PublicKey) {
        val calls = mutableSetOf<Contact>().apply { addAll(_pendingCalls.value) }
        val removed = calls.firstOrNull { it.publicKey == pk.string() }
        if (removed != null) {
            Log.i(TAG, "Removed pending call ${pk.fingerprint()}")
            calls.remove(removed)
            _pendingCalls.value = calls
        }
    }

    private var toneGenerator: ToneGenerator? = null
    private var isConnected = false

    private fun playTone(toneType: Int, durationMs: Int = -1) {
        scope.launch {
            try {
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100)
                }
                toneGenerator?.startTone(toneType, durationMs)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing tone: $e")
            }
        }
    }

    private fun stopTone() {
        scope.launch {
            try {
                toneGenerator?.stopTone()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping tone: $e")
            }
        }
    }

    fun onCallConnected() {
        if (!isConnected) {
            isConnected = true
            stopTone()
            playTone(ToneGenerator.TONE_PROP_BEEP, 200)
        }
    }

    fun startCall(publicKey: PublicKey) {
        isConnected = false
        val isOutgoing = !pendingCalls.value.any { it.publicKey == publicKey.string() }
        if (isOutgoing) {
            playTone(ToneGenerator.TONE_SUP_RINGTONE)
        }
        if (pendingCalls.value.any { it.publicKey == publicKey.string() }) {
            tox.answerCall(publicKey)
        } else {
            tox.startCall(publicKey)
        }
        _inCall.value = CallState.InCall(publicKey, SystemClock.elapsedRealtime())
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        removePendingCall(publicKey)
    }

    fun endCall(publicKey: PublicKey) {
        val state = inCall.value
        val isCurrentCall = state is CallState.InCall && state.publicKey == publicKey
        val isPendingCall = pendingCalls.value.any { it.publicKey == publicKey.string() }

        if (!isCurrentCall && !isPendingCall) {
            return
        }

        terminate(publicKey)

        try {
            tox.endCall(publicKey)
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call: $e")
        }
    }

    fun terminate(publicKey: PublicKey) {
        isConnected = false
        stopTone()
        playTone(ToneGenerator.TONE_PROP_PROMPT, 200)

        val state = inCall.value
        if (state is CallState.InCall && state.publicKey == publicKey) {
            audioManager?.mode = AudioManager.MODE_NORMAL
            _inCall.value = CallState.NotInCall
        }

        removePendingCall(publicKey)
    }

    fun startSendingAudio(): Boolean {
        val to = (inCall.value as CallState.InCall?)?.publicKey ?: return false
        val recorder =
            AudioCapture.create(AUDIO_SAMPLING_RATE_HZ, AUDIO_CHANNELS, AUDIO_SEND_INTERVAL_MS) ?: return false
        startAudioSender(recorder, to)
        return true
    }

    fun stopSendingAudio() {
        _sendingAudio.value = false
    }

    var speakerphoneOn: Boolean
        get() = audioManager?.isSpeakerphoneOn ?: false
        set(value) {
            audioManager?.isSpeakerphoneOn = value
        }

    private fun startAudioSender(recorder: AudioCapture, to: PublicKey) {
        scope.launch {
            recorder.start()
            _sendingAudio.value = true
            while (inCall.value is CallState.InCall && sendingAudio.value) {
                val start = System.currentTimeMillis()
                val audioFrame = recorder.read()
                try {
                    tox.sendAudio(to, audioFrame, AUDIO_CHANNELS, AUDIO_SAMPLING_RATE_HZ)
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
                val elapsed = System.currentTimeMillis() - start
                if (elapsed < AUDIO_SEND_INTERVAL_MS) {
                    delay(AUDIO_SEND_INTERVAL_MS - elapsed)
                }
            }
            recorder.stop()
            recorder.release()
            _sendingAudio.value = false
        }
    }
}
