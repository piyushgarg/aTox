// SPDX-FileCopyrightText: 2021-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.PublicKey

import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.CallState
import ltd.evilcorp.domain.feature.ContactManager
import ltd.evilcorp.domain.feature.NotificationManager
import ltd.evilcorp.domain.feature.ProximityManager

import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callManager: CallManager,
    private val notificationManager: NotificationManager,
    private val contactManager: ContactManager,
    private val proximityManager: ProximityManager,
) : ViewModel() {
    private var publicKey = PublicKey("")
    private val activePublicKey = MutableStateFlow<PublicKey?>(null)

    private val _callDuration = MutableStateFlow("00:00")
    val callDuration = _callDuration.asStateFlow()

    private var durationJob: Job? = null

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val contact: StateFlow<Contact?> = activePublicKey
        .filterNotNull()
        .flatMapLatest { pk -> contactManager.get(pk) }
        
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            callManager.inCall.collect { state ->
                durationJob?.cancel()
                if (state is CallState.Active) {
                    val startTime = state.connectedAt
                    durationJob = viewModelScope.launch {
                        while (true) {
                            val elapsedMs = System.currentTimeMillis() - startTime
                            val elapsedSec = elapsedMs / 1000
                            val minutes = elapsedSec / 60
                            val seconds = elapsedSec % 60
                            _callDuration.value = String.format("%02d:%02d", minutes, seconds)
                            delay(1000)
                        }
                    }
                } else {
                    _callDuration.value = "00:00"
                }
            }
        }
    }

    fun setActiveContact(pk: PublicKey) {
        publicKey = pk
        activePublicKey.value = pk
    }

    fun startCall() {
        viewModelScope.launch {
            val state = callManager.inCall.value
            val started = when (state) {
                CallState.Idle -> callManager.startOutgoingCall(publicKey)
                is CallState.OutgoingRequesting -> state.publicKey == publicKey
                is CallState.OutgoingWaiting -> state.publicKey == publicKey
                is CallState.OutgoingRinging -> state.publicKey == publicKey
                is CallState.Connecting -> state.publicKey == publicKey
                is CallState.Active -> state.publicKey == publicKey
                is CallState.IncomingRinging -> false
            }

            if (!started) {
                return@launch
            }

            callManager.startSendingAudio()
            notificationManager.showOngoingCallNotification(contactManager.get(publicKey).first() ?: Contact(publicKey.string()))
        }
    }

    fun endCall() = viewModelScope.launch {
        callManager.endCall(publicKey)
        notificationManager.dismissCallNotification(publicKey)
    }

    fun startSendingAudio() = callManager.startSendingAudio()
    fun stopSendingAudio() = callManager.stopSendingAudio()

    val speakerphoneState = callManager.speakerphoneOnState

    fun toggleSpeakerphone() {
        callManager.toggleSpeakerphone()
        if (speakerphoneState.value) {
            proximityManager.release()
        } else {
            proximityManager.acquire()
        }
    }

    val inCall = callManager.inCall
    val sendingAudio = callManager.microphoneEnabled
    val connectedAt = callManager.inCall.map { (it as? CallState.Active)?.connectedAt ?: -1L }
}
