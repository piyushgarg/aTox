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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.call.CallState
import ltd.evilcorp.domain.features.contacts.usecase.GetContactUseCase
import ltd.evilcorp.domain.features.call.usecase.GetCallStateUseCase
import ltd.evilcorp.domain.features.call.usecase.GetSpeakerphoneStateUseCase
import ltd.evilcorp.domain.features.call.usecase.GetMicrophoneStateUseCase
import ltd.evilcorp.domain.features.call.usecase.ManageCallUseCase
import ltd.evilcorp.domain.features.call.usecase.CallAction
import dagger.hilt.android.lifecycle.HiltViewModel

private const val MILLIS_IN_SECOND = 1000L
private const val SECONDS_IN_MINUTE = 60L
private const val UPDATE_DELAY_MS = 1000L

@HiltViewModel
class CallViewModel @Inject constructor(
    private val getContactUseCase: GetContactUseCase,
    private val getCallStateUseCase: GetCallStateUseCase,
    private val getSpeakerphoneStateUseCase: GetSpeakerphoneStateUseCase,
    private val getMicrophoneStateUseCase: GetMicrophoneStateUseCase,
    private val manageCallUseCase: ManageCallUseCase,
) : ViewModel() {
    private var publicKey = PublicKey("")
    private val activePublicKey = MutableStateFlow<PublicKey?>(null)

    private val _callDuration = MutableStateFlow("00:00")
    val callDuration = _callDuration.asStateFlow()

    private var durationJob: Job? = null

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val contact: StateFlow<Contact?> = activePublicKey
        .filterNotNull()
        .flatMapLatest { pk -> getContactUseCase.execute(pk) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            getCallStateUseCase.inCall.collect { state ->
                durationJob?.cancel()
                if (state is CallState.Active) {
                    val startTime = state.connectedAt
                    durationJob = viewModelScope.launch {
                        while (true) {
                            val elapsedMs = System.currentTimeMillis() - startTime
                            val elapsedSec = elapsedMs / MILLIS_IN_SECOND
                            val minutes = elapsedSec / SECONDS_IN_MINUTE
                            val seconds = elapsedSec % SECONDS_IN_MINUTE
                            _callDuration.value = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
                            delay(UPDATE_DELAY_MS)
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
            val contactValue = getContactUseCase.execute(publicKey).first() ?: Contact(publicKey.string())
            manageCallUseCase.execute(CallAction.StartOutgoingCall(publicKey, contactValue))
        }
    }

    fun endCall() = viewModelScope.launch {
        manageCallUseCase.execute(CallAction.EndCall(publicKey))
    }

    fun startSendingAudio() {
        viewModelScope.launch {
            manageCallUseCase.execute(CallAction.StartSendingAudio)
        }
    }
    fun stopSendingAudio() {
        viewModelScope.launch {
            manageCallUseCase.execute(CallAction.StopSendingAudio)
        }
    }

    val speakerphoneState = getSpeakerphoneStateUseCase.speakerphoneOnState

    fun toggleSpeakerphone() {
        viewModelScope.launch {
            manageCallUseCase.execute(CallAction.ToggleSpeakerphone)
        }
    }

    val inCall = getCallStateUseCase.inCall
    val sendingAudio = getMicrophoneStateUseCase.microphoneEnabled
    val connectedAt = getCallStateUseCase.inCall.map { (it as? CallState.Active)?.connectedAt ?: -1L }
}
