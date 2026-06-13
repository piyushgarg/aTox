// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import ltd.evilcorp.domain.features.call.CallManager
import ltd.evilcorp.domain.features.call.CallState
import ltd.evilcorp.domain.features.contacts.ContactManager
import ltd.evilcorp.domain.core.network.INotificationHelper
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.core.model.PublicKey
import javax.inject.Inject
import ltd.evilcorp.atox.ui.common.debounceOffline

class ChatCallDelegate @Inject constructor(
    private val callManager: CallManager,
    private val notificationHelper: INotificationHelper,
    private val contactManager: ContactManager,
) {
    val ongoingCall = callManager.inCall

    suspend fun startCall(publicKey: PublicKey) {
        if (callManager.startOutgoingCall(publicKey)) {
            callManager.startSendingAudio()
            val contact = contactManager.get(publicKey).take(1).first() ?: Contact(publicKey.string())
            notificationHelper.showOngoingCallNotification(contact)
        }
    }

    fun endCall(publicKey: PublicKey) {
        callManager.endCall(publicKey)
        notificationHelper.dismissCallNotification(publicKey)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCallState(activePublicKey: Flow<PublicKey?>): Flow<CallAvailability> {
        return activePublicKey
            .filterNotNull()
            .flatMapLatest { pk ->
                contactManager.get(pk)
                    .debounceOffline()
                    .filterNotNull()
                    .transform { emit(it.connectionStatus != ConnectionStatus.None) }
                    .combine(callManager.inCall) { contactOnline, callState ->
                        if (!contactOnline) return@combine CallAvailability.Unavailable
                        when (callState) {
                            CallState.Idle -> CallAvailability.Available
                            is CallState.IncomingRinging ->
                                if (callState.contact.publicKey == pk.string()) CallAvailability.Active else CallAvailability.Unavailable
                            is CallState.OutgoingRequesting ->
                                if (callState.publicKey == pk) CallAvailability.Active else CallAvailability.Unavailable
                            is CallState.OutgoingWaiting ->
                                if (callState.publicKey == pk) CallAvailability.Active else CallAvailability.Unavailable
                            is CallState.Connecting ->
                                if (callState.publicKey == pk) CallAvailability.Active else CallAvailability.Unavailable
                            is CallState.OutgoingRinging ->
                                if (callState.publicKey == pk) CallAvailability.Active else CallAvailability.Unavailable
                            is CallState.Active ->
                                if (callState.publicKey == pk) CallAvailability.Active else CallAvailability.Unavailable
                        }
                    }
            }
    }
}
