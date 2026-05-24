// SPDX-FileCopyrightText: 2021-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.feature

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.MessageRepository
import ltd.evilcorp.core.repository.UserSettingsRepository
import ltd.evilcorp.domain.R
import ltd.evilcorp.domain.av.AudioCapture
import ltd.evilcorp.domain.tox.Tox

sealed class CallState {
    object Idle : CallState()
    data class IncomingRinging(val contact: Contact, val startedAt: Long) : CallState()
    data class OutgoingRequesting(val publicKey: PublicKey, val startedAt: Long) : CallState()
    data class OutgoingWaiting(val publicKey: PublicKey, val startedAt: Long) : CallState()
    data class OutgoingRinging(val publicKey: PublicKey, val startedAt: Long) : CallState()
    data class Connecting(val publicKey: PublicKey, val startedAt: Long, val outgoing: Boolean) : CallState()
    data class Active(val publicKey: PublicKey, val startedAt: Long, val connectedAt: Long, val outgoing: Boolean) : CallState()
}

private const val TAG = "CallManager"
private const val AUDIO_CHANNELS = 1
private const val AUDIO_SAMPLING_RATE_HZ = 48_000
private const val AUDIO_SEND_INTERVAL_MS = 20
private const val RINGBACK_TONE_DURATION_MS = 1_500
private const val RINGBACK_TONE_INTERVAL_MS = 2_000L

@Singleton
class CallManager @Inject constructor(
    private val tox: Tox,
    private val scope: CoroutineScope,
    private val userSettingsRepository: UserSettingsRepository,
    private val context: Context,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
) {
    private val _inCall = MutableStateFlow<CallState>(CallState.Idle)
    val inCall: StateFlow<CallState> get() = _inCall

    private val _sendingAudio = MutableStateFlow(false)
    val sendingAudio: StateFlow<Boolean> get() = _sendingAudio

    private val _speakerphoneOn = MutableStateFlow(false)
    val speakerphoneOnState: StateFlow<Boolean> get() = _speakerphoneOn

    private val audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)
    private var ringtonePlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var ringbackJob: Job? = null
    private var transitionJob: Job? = null
    private var audioCaptureJob: Job? = null
    private var microphoneDesired = true

    suspend fun startOutgoingCall(publicKey: PublicKey): Boolean {
        if (_inCall.value != CallState.Idle) return false

        val contact = contactRepository.get(publicKey.string()).first() ?: return false
        if (contact.connectionStatus == ConnectionStatus.None) {
            return false
        }

        return try {
            microphoneDesired = true
            tox.startCall(publicKey)
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            setState(CallState.OutgoingRequesting(publicKey, SystemClock.elapsedRealtime()))
            playRingback()
            transitionJob?.cancel()
            transitionJob = scope.launch {
                delay(500)
                if (_inCall.value is CallState.OutgoingRequesting) {
                    setState(CallState.OutgoingWaiting(publicKey, SystemClock.elapsedRealtime()))
                }
                delay(900)
                if (_inCall.value is CallState.OutgoingWaiting) {
                    setState(CallState.OutgoingRinging(publicKey, SystemClock.elapsedRealtime()))
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting outgoing call", e)
            cleanupSession()
            false
        }
    }

    fun onIncomingCall(from: Contact) {
        if (_inCall.value != CallState.Idle) {
            scope.launch {
                runCatching { tox.endCall(PublicKey(from.publicKey)) }
            }
            return
        }
        microphoneDesired = true
        setState(CallState.IncomingRinging(from, SystemClock.elapsedRealtime()))
        playIncomingRingtone()
    }

    fun removePendingCall(publicKey: PublicKey) {
        val incoming = _inCall.value as? CallState.IncomingRinging ?: return
        if (incoming.contact.publicKey != publicKey.string()) return
        finishSession(publicKey, localHangup = true, record = CallHistory.Missed)
    }

    suspend fun acceptIncomingCall(publicKey: PublicKey): Boolean {
        val incoming = _inCall.value as? CallState.IncomingRinging ?: return false
        if (incoming.contact.publicKey != publicKey.string()) return false
        return try {
            stopSignals()
            tox.answerCall(publicKey)
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            setState(CallState.Connecting(publicKey, incoming.startedAt, outgoing = false))
            onCallConnected(publicKey)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error answering incoming call", e)
            cleanupSession()
            false
        }
    }

    fun onRemoteAnswered(publicKey: PublicKey) {
        val current = _inCall.value
        when (current) {
            is CallState.OutgoingRequesting ->
                if (current.publicKey == publicKey) setState(CallState.Connecting(publicKey, current.startedAt, outgoing = true))
            is CallState.OutgoingWaiting ->
                if (current.publicKey == publicKey) setState(CallState.Connecting(publicKey, current.startedAt, outgoing = true))
            is CallState.OutgoingRinging ->
                if (current.publicKey == publicKey) setState(CallState.Connecting(publicKey, current.startedAt, outgoing = true))
            else -> Unit
        }
    }

    fun onCallConnected(publicKey: PublicKey) {
        val current = _inCall.value
        val nextState = when (current) {
            is CallState.Connecting ->
                if (current.publicKey == publicKey) {
                    CallState.Active(publicKey, current.startedAt, SystemClock.elapsedRealtime(), current.outgoing)
                } else {
                    null
                }
            is CallState.IncomingRinging ->
                if (current.contact.publicKey == publicKey.string()) {
                    CallState.Active(publicKey, current.startedAt, SystemClock.elapsedRealtime(), outgoing = false)
                } else {
                    null
                }
            else -> null
        } ?: return

        stopSignals()
        setState(nextState)
        if (microphoneDesired) {
            startAudioCapture(publicKey)
        }
    }

    fun endCall(publicKey: PublicKey) {
        val record = when (val current = _inCall.value) {
            is CallState.IncomingRinging ->
                if (current.contact.publicKey == publicKey.string()) CallHistory.Missed else null
            is CallState.OutgoingRequesting -> null
            is CallState.OutgoingWaiting -> CallHistory.Cancelled
            is CallState.OutgoingRinging -> CallHistory.Cancelled
            is CallState.Connecting -> if (current.publicKey == publicKey) {
                if (current.outgoing) CallHistory.Cancelled else CallHistory.Incoming
            } else null
            is CallState.Active -> if (current.publicKey == publicKey) {
                if (current.outgoing) CallHistory.Outgoing else CallHistory.Incoming
            } else null
            CallState.Idle -> null
        }
        finishSession(publicKey, localHangup = true, record = record)
    }

    fun terminate(publicKey: PublicKey) {
        val record = when (val current = _inCall.value) {
            is CallState.IncomingRinging ->
                if (current.contact.publicKey == publicKey.string()) CallHistory.Missed else null
            is CallState.OutgoingRequesting -> null
            is CallState.OutgoingWaiting -> CallHistory.Cancelled
            is CallState.OutgoingRinging -> CallHistory.Cancelled
            is CallState.Connecting -> if (current.publicKey == publicKey) {
                if (current.outgoing) CallHistory.Cancelled else CallHistory.Incoming
            } else null
            is CallState.Active -> if (current.publicKey == publicKey) {
                if (current.outgoing) CallHistory.Outgoing else CallHistory.Incoming
            } else null
            CallState.Idle -> null
        }
        finishSession(publicKey, localHangup = false, record = record)
    }

    fun startSendingAudio(): Boolean {
        microphoneDesired = true
        val target = currentTarget() ?: return false
        if (_inCall.value is CallState.Active) {
            startAudioCapture(target)
        }
        return true
    }

    fun stopSendingAudio() {
        microphoneDesired = false
        _sendingAudio.value = false
    }

    fun toggleSpeakerphone() {
        val next = !_speakerphoneOn.value
        _speakerphoneOn.value = next
        audioManager?.isSpeakerphoneOn = next
        restartAudioCaptureForRouteChange()
    }

    private fun finishSession(publicKey: PublicKey, localHangup: Boolean, record: CallHistory?) {
        scope.launch {
            if (localHangup) {
                runCatching { tox.endCall(publicKey) }
            }
            if (record != null) {
                logCall(publicKey, record)
            }
            cleanupSession()
        }
    }

    private fun setState(state: CallState) {
        _inCall.value = state
    }

    private fun cleanupSession() {
        transitionJob?.cancel()
        transitionJob = null
        microphoneDesired = true
        _sendingAudio.value = false
        audioCaptureJob?.cancel()
        audioCaptureJob = null
        stopSignals()
        _speakerphoneOn.value = false
        audioManager?.isSpeakerphoneOn = false
        audioManager?.mode = AudioManager.MODE_NORMAL
        setState(CallState.Idle)
    }

    private fun playIncomingRingtone() {
        scope.launch {
            stopSignals()
            try {
                val settings = userSettingsRepository.settings.value
                val uri = settings.callRingtoneUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val volume = settings.callSoundVolume.coerceIn(0, 100) / 100f
                ringtonePlayer = MediaPlayer().apply {
                    setDataSource(context, uri)
                    isLooping = true
                    setAudioStreamType(AudioManager.STREAM_RING)
                    setVolume(volume, volume)
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing ringtone", e)
            }
        }
    }

    private fun playRingback() {
        scope.launch {
            stopSignals()
            toneGenerator = ToneGenerator(
                AudioManager.STREAM_VOICE_CALL,
                userSettingsRepository.settings.value.callSoundVolume.coerceIn(0, 100),
            )
            ringbackJob = scope.launch {
                while (_inCall.value is CallState.OutgoingRequesting ||
                    _inCall.value is CallState.OutgoingWaiting ||
                    _inCall.value is CallState.OutgoingRinging
                ) {
                    runCatching { toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, RINGBACK_TONE_DURATION_MS) }
                    delay(RINGBACK_TONE_INTERVAL_MS)
                }
            }
        }
    }

    private fun stopSignals() {
        transitionJob?.cancel()
        ringbackJob?.cancel()
        ringbackJob = null
        runCatching { toneGenerator?.stopTone() }
        toneGenerator?.release()
        toneGenerator = null
        runCatching { ringtonePlayer?.stop() }
        runCatching { ringtonePlayer?.release() }
        ringtonePlayer = null
    }

    private fun currentTarget(): PublicKey? = when (val current = _inCall.value) {
        is CallState.OutgoingRequesting -> current.publicKey
        is CallState.OutgoingWaiting -> current.publicKey
        is CallState.OutgoingRinging -> current.publicKey
        is CallState.Connecting -> current.publicKey
        is CallState.Active -> current.publicKey
        is CallState.IncomingRinging -> PublicKey(current.contact.publicKey)
        CallState.Idle -> null
    }

    private fun startAudioCapture(to: PublicKey) {
        if (audioCaptureJob?.isActive == true) return
        val recorder = AudioCapture.create(
            AUDIO_SAMPLING_RATE_HZ,
            AUDIO_CHANNELS,
            AUDIO_SEND_INTERVAL_MS,
            enableAutomaticGainControl = !_speakerphoneOn.value,
        ) ?: return
        audioCaptureJob = scope.launch {
            recorder.start()
            _sendingAudio.value = true
            try {
                while (_sendingAudio.value && currentTarget() == to) {
                    val start = System.currentTimeMillis()
                    val audioFrame = recorder.read()
                    if (audioFrame != null) {
                        runCatching { tox.sendAudio(to, audioFrame, AUDIO_CHANNELS, AUDIO_SAMPLING_RATE_HZ) }
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

    private fun restartAudioCaptureForRouteChange() {
        val target = currentTarget() ?: return
        if (_inCall.value !is CallState.Active || !microphoneDesired || !_sendingAudio.value) return

        _sendingAudio.value = false
        scope.launch {
            audioCaptureJob?.join()
            if (_inCall.value is CallState.Active && microphoneDesired && currentTarget() == target) {
                startAudioCapture(target)
            }
        }
    }

    private fun logCall(publicKey: PublicKey, event: CallHistory) {
        val tag = when (event) {
            CallHistory.Outgoing -> "[CALL_HISTORY_OUTGOING]"
            CallHistory.Incoming -> "[CALL_HISTORY_INCOMING]"
            CallHistory.Missed -> "[CALL_HISTORY_MISSED]"
            CallHistory.Cancelled -> "[CALL_HISTORY_CANCELLED]"
        }
        val sender = when (event) {
            CallHistory.Outgoing, CallHistory.Cancelled -> Sender.Sent
            CallHistory.Incoming, CallHistory.Missed -> Sender.Received
        }
        messageRepository.add(
            Message(
                publicKey = publicKey.string(),
                message = tag,
                sender = sender,
                type = MessageType.Action,
                correlationId = Int.MIN_VALUE,
            ),
        )
    }

    private enum class CallHistory {
        Outgoing,
        Incoming,
        Missed,
        Cancelled,
    }
}
