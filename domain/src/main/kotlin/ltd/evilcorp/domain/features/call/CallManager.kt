// SPDX-FileCopyrightText: 2021-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.call
import ltd.evilcorp.domain.core.network.Log

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.call.service.ICallSignalPlayer
import ltd.evilcorp.domain.features.call.service.IAudioRecorder
import ltd.evilcorp.domain.core.network.IToxCallController
import ltd.evilcorp.domain.features.call.usecase.LogCallUseCase
import ltd.evilcorp.domain.features.call.usecase.CallHistoryType

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
private const val TRANSITION_WAITING_DELAY_MS = 500L
private const val TRANSITION_RINGING_DELAY_MS = 900L

@Singleton
class CallManager @Inject constructor(
    private val tox: IToxCallController,
    private val scope: CoroutineScope,
    private val contactRepository: IContactRepository,
    private val logCallUseCase: LogCallUseCase,
    private val signalPlayer: ICallSignalPlayer,
    private val audioRecorder: IAudioRecorder,
    private val audioRoutingManager: IAudioRoutingManager,
) {
    private val _inCall = MutableStateFlow<CallState>(CallState.Idle)
    val inCall: StateFlow<CallState> get() = _inCall

    val sendingAudio: StateFlow<Boolean> get() = audioRecorder.sendingAudio

    private val _speakerphoneOn = MutableStateFlow(false)
    val speakerphoneOnState: StateFlow<Boolean> get() = _speakerphoneOn

    private val _microphoneEnabled = MutableStateFlow(true)
    val microphoneEnabled: StateFlow<Boolean> get() = _microphoneEnabled

    private var transitionJob: Job? = null

    private fun requestAudioFocus(): Boolean {
        return audioRoutingManager.requestCallAudioFocus(
            onFocusLoss = {
                audioRecorder.stopAudioCapture()
            },
            onFocusGain = {
                val target = currentTarget()
                if (target != null && _inCall.value is CallState.Active && _microphoneEnabled.value) {
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
            _microphoneEnabled.value = true
            requestAudioFocus()
            tox.startCall(publicKey)
            audioRoutingManager.setCommunicationMode(true)
            setState(CallState.OutgoingRequesting(publicKey, System.currentTimeMillis()))
            signalPlayer.playRingback(scope) {
                val state = _inCall.value
                state is CallState.OutgoingRequesting ||
                    state is CallState.OutgoingWaiting ||
                    state is CallState.OutgoingRinging
            }
            transitionJob?.cancel()
            transitionJob = scope.launch {
                delay(TRANSITION_WAITING_DELAY_MS)
                if (_inCall.value is CallState.OutgoingRequesting) {
                    setState(CallState.OutgoingWaiting(publicKey, System.currentTimeMillis()))
                }
                delay(TRANSITION_RINGING_DELAY_MS)
                if (_inCall.value is CallState.OutgoingWaiting) {
                    setState(CallState.OutgoingRinging(publicKey, System.currentTimeMillis()))
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting outgoing call: $e")
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
        _microphoneEnabled.value = true
        setState(CallState.IncomingRinging(from, System.currentTimeMillis()))
        signalPlayer.playIncomingRingtone(scope)
    }

    fun removePendingCall(publicKey: PublicKey) {
        val incoming = _inCall.value as? CallState.IncomingRinging ?: return
        if (incoming.contact.publicKey != publicKey.string()) return
        finishSession(publicKey, localHangup = true, record = CallHistoryType.Missed)
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
            Log.e(TAG, "Error answering incoming call: $e")
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
                    CallState.Active(publicKey, current.startedAt, System.currentTimeMillis(), current.outgoing)
                } else {
                    null
                }
            is CallState.IncomingRinging ->
                if (current.contact.publicKey == publicKey.string()) {
                    CallState.Active(publicKey, current.startedAt, System.currentTimeMillis(), outgoing = false)
                } else {
                    null
                }
            else -> null
        } ?: return

        signalPlayer.stopSignals()
        setState(nextState)
        if (_microphoneEnabled.value) {
            startAudioCapture(publicKey)
        }
    }

    fun endCall(publicKey: PublicKey) {
        val record = when (val current = _inCall.value) {
            is CallState.IncomingRinging ->
                if (current.contact.publicKey == publicKey.string()) CallHistoryType.Missed else null
            is CallState.OutgoingRequesting -> null
            is CallState.OutgoingWaiting -> CallHistoryType.Cancelled
            is CallState.OutgoingRinging -> CallHistoryType.Cancelled
            is CallState.Connecting -> if (current.publicKey == publicKey) {
                if (current.outgoing) CallHistoryType.Cancelled else CallHistoryType.Incoming
            } else null
            is CallState.Active -> if (current.publicKey == publicKey) {
                if (current.outgoing) CallHistoryType.Outgoing else CallHistoryType.Incoming
            } else null
            CallState.Idle -> null
        }
        finishSession(publicKey, localHangup = true, record = record)
    }

    fun terminate(publicKey: PublicKey) {
        val record = when (val current = _inCall.value) {
            is CallState.IncomingRinging ->
                if (current.contact.publicKey == publicKey.string()) CallHistoryType.Missed else null
            is CallState.OutgoingRequesting -> null
            is CallState.OutgoingWaiting -> CallHistoryType.Cancelled
            is CallState.OutgoingRinging -> CallHistoryType.Cancelled
            is CallState.Connecting -> if (current.publicKey == publicKey) {
                if (current.outgoing) CallHistoryType.Cancelled else CallHistoryType.Incoming
            } else null
            is CallState.Active -> if (current.publicKey == publicKey) {
                if (current.outgoing) CallHistoryType.Outgoing else CallHistoryType.Incoming
            } else null
            CallState.Idle -> null
        }
        finishSession(publicKey, localHangup = false, record = record)
    }

    fun startSendingAudio(): Boolean {
        _microphoneEnabled.value = true
        val target = currentTarget() ?: return false
        if (_inCall.value is CallState.Active) {
            startAudioCapture(target)
        }
        return true
    }

    fun stopSendingAudio() {
        _microphoneEnabled.value = false
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

    private fun finishSession(publicKey: PublicKey, localHangup: Boolean, record: CallHistoryType?) {
        scope.launch {
            if (localHangup) {
                runCatching { tox.endCall(publicKey) }
            }
            if (record != null) {
                logCallUseCase.execute(publicKey.string(), record)
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
        _microphoneEnabled.value = true
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
        if (_inCall.value !is CallState.Active || !_microphoneEnabled.value || !audioRecorder.sendingAudio.value) return

        audioRecorder.stopAudioCapture()
        scope.launch {
            audioRecorder.joinRecording()
            if (_inCall.value is CallState.Active && _microphoneEnabled.value && currentTarget() == target) {
                startAudioCapture(target)
            }
        }
    }
}
