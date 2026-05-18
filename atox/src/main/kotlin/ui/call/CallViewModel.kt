// SPDX-FileCopyrightText: 2021-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.call

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.service.ProximityScreenOff
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.CallState
import ltd.evilcorp.domain.feature.ContactManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

class CallViewModel @Inject constructor(
    private val scope: CoroutineScope,
    private val callManager: CallManager,
    private val notificationHelper: NotificationHelper,
    private val contactManager: ContactManager,
    private val proximityScreenOff: ProximityScreenOff,
) : ViewModel() {
    private var publicKey = PublicKey("")
    private val activePublicKey = MutableStateFlow<PublicKey?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val contact: LiveData<Contact?> = activePublicKey
        .filterNotNull()
        .flatMapLatest { pk -> contactManager.get(pk) }
        .asLiveData()

    fun setActiveContact(pk: PublicKey) {
        publicKey = pk
        activePublicKey.value = pk
    }

    fun startCall() {
        if (callManager.inCall.value !is CallState.InCall) {
            callManager.startCall(publicKey)
        }
        callManager.startSendingAudio()
        scope.launch { notificationHelper.showOngoingCallNotification(contactManager.get(publicKey).first() ?: Contact(publicKey.string())) }
    }

    fun endCall() = scope.launch {
        callManager.endCall(publicKey)
        notificationHelper.dismissCallNotification(publicKey)
    }

    fun startSendingAudio() = callManager.startSendingAudio()
    fun stopSendingAudio() = callManager.stopSendingAudio()

    val speakerphoneState = MutableStateFlow(callManager.speakerphoneOn)

    fun toggleSpeakerphone() {
        speakerphoneOn = !speakerphoneOn
        speakerphoneState.value = speakerphoneOn
        if (speakerphoneOn) {
            proximityScreenOff.release()
        } else {
            proximityScreenOff.acquire()
        }
    }

    val inCall = callManager.inCall
    val sendingAudio = callManager.sendingAudio

    var speakerphoneOn by callManager::speakerphoneOn
}
