// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.call.usecase

import ltd.evilcorp.domain.features.call.CallManager
import ltd.evilcorp.domain.features.call.CallState
import ltd.evilcorp.domain.features.call.IProximityManager
import ltd.evilcorp.domain.core.network.INotificationManager
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.core.model.PublicKey
import javax.inject.Inject

sealed interface CallAction {
    data class StartOutgoingCall(val publicKey: PublicKey, val contact: Contact) : CallAction
    data class EndCall(val publicKey: PublicKey) : CallAction
    object StartSendingAudio : CallAction
    object StopSendingAudio : CallAction
    object ToggleSpeakerphone : CallAction
}

/**
 * Use case to manage call operations and speakerphone/microphone audio routing states.
 */
class ManageCallUseCase @Inject constructor(
    private val callManager: CallManager,
    private val notificationManager: INotificationManager,
    private val proximityManager: IProximityManager,
) {
    suspend fun execute(action: CallAction): Boolean {
        return when (action) {
            is CallAction.StartOutgoingCall -> {
                val state = callManager.inCall.value
                val started = when (state) {
                    CallState.Idle -> callManager.startOutgoingCall(action.publicKey)
                    is CallState.OutgoingRequesting -> state.publicKey == action.publicKey
                    is CallState.OutgoingWaiting -> state.publicKey == action.publicKey
                    is CallState.OutgoingRinging -> state.publicKey == action.publicKey
                    is CallState.Connecting -> state.publicKey == action.publicKey
                    is CallState.Active -> state.publicKey == action.publicKey
                    is CallState.IncomingRinging -> false
                }
                if (started) {
                    callManager.startSendingAudio()
                    notificationManager.showOngoingCallNotification(action.contact)
                }
                started
            }
            is CallAction.EndCall -> {
                callManager.endCall(action.publicKey)
                notificationManager.dismissCallNotification(action.publicKey)
                true
            }
            is CallAction.StartSendingAudio -> {
                callManager.startSendingAudio()
                true
            }
            is CallAction.StopSendingAudio -> {
                callManager.stopSendingAudio()
                true
            }
            is CallAction.ToggleSpeakerphone -> {
                callManager.toggleSpeakerphone()
                if (callManager.speakerphoneOnState.value) {
                    proximityManager.release()
                } else {
                    proximityManager.acquire()
                }
                true
            }
        }
    }
}
