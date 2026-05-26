// SPDX-FileCopyrightText: 2021-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.feature

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
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
import ltd.evilcorp.domain.repository.IContactRepository
import ltd.evilcorp.domain.repository.IMessageRepository
import ltd.evilcorp.domain.media.CallSignalPlayer
import ltd.evilcorp.domain.av.CallAudioRecorder
import ltd.evilcorp.domain.tox.ITox

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

@Singleton
class CallManager @Inject constructor(
    private val tox: ITox,
    private val scope: CoroutineScope,
    private val context: Context,
    private val contactRepository: IContactRepository,
    private val messageRepository: IMessageRepository,
    private val signalPlayer: CallSignalPlayer,
    private val audioRecorder: CallAudioRecorder,
    private val audioRoutingManager: IAudioRoutingManager,
) {
    private val _inCall = MutableStateFlow<CallState>(CallState.Idle)
    val inCall: StateFlow<CallState> get() = _inCall

    val sendingAudio: StateFlow<Boolean> get() = audioRecorder.sendingAudio

    private val _speakerphoneOn = MutableStateFlow(false)
    val speakerphoneOnState: StateFlow<Boolean> get() = _speakerphoneOn

    private var transitionJob: Job? = null
    private var microphoneDesired = true

    private fun requestAudioFocus(): Boolean {
        return audioRoutingManager.requestCallAudioFocus(
            onFocusLoss = {
                audioRecorder.stopAudioCapture()
            },
            onFocusGain = {
                val target = currentTarget()
                if (target != null && _inCall.value is CallState.Active && microphoneDesired) {
                    startAudioCapture(target)
                }
            }
        )
    }

    private fun abandonAudioFocus() {
        audioRoutingManager.abandonCallAudioFocus()
    }

    suspend fun startOutgoingCall(publicKey: PublicKey): Boolean {
        if (_inCall.value != CallState.Idle) return false

        val contact = contactRepository.get(publicKey.string()).first() ?: return false
        if (contact.connectionStatus == ConnectionStatus.None) {
            return false
        }

        return try {
            microphoneDesired = true
            requestAudioFocus()
            tox.startCall(publicKey)
            audioRoutingManager.setCommunicationMode(true)
            setState(CallState.OutgoingRequesting(publicKey, SystemClock.elapsedRealtime()))
            signalPlayer.playRingback(scope) {
                val state = _inCall.value
                state is CallState.OutgoingRequesting ||
                    state is CallState.OutgoingWaiting ||
                    state is CallState.OutgoingRinging
            }
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
        signalPlayer.playIncomingRingtone(scope)
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
            signalPlayer.stopSignals()
            requestAudioFocus()
            tox.answerCall(publicKey)
            audioRoutingManager.setCommunicationMode(true)
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

        signalPlayer.stopSignals()
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
        audioRecorder.stopAudioCapture()
    }

    fun toggleSpeakerphone() {
        val next = !_speakerphoneOn.value
        _speakerphoneOn.value = next
        setSpeakerphoneRoute(next)
        restartAudioCaptureForRouteChange()
    }

    private fun setSpeakerphoneRoute(on: Boolean) {
        audioRoutingManager.setSpeakerphoneRoute(on)
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
        audioRecorder.stopAudioCapture()
        signalPlayer.stopSignals()
        abandonAudioFocus()
        _speakerphoneOn.value = false
        setSpeakerphoneRoute(false)
        audioRoutingManager.setCommunicationMode(false)
        setState(CallState.Idle)
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
        audioRecorder.startAudioCapture(scope, to, _speakerphoneOn.value) {
            currentTarget() == to
        }
    }

    private fun restartAudioCaptureForRouteChange() {
        val target = currentTarget() ?: return
        if (_inCall.value !is CallState.Active || !microphoneDesired || !audioRecorder.sendingAudio.value) return

        audioRecorder.stopAudioCapture()
        scope.launch {
            audioRecorder.joinRecording()
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
