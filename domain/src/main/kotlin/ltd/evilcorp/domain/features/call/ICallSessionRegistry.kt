package ltd.evilcorp.domain.features.call

import kotlinx.coroutines.flow.StateFlow

interface ICallSessionRegistry {
    val inCall: StateFlow<CallState>
    val speakerphoneOn: StateFlow<Boolean>
    val microphoneEnabled: StateFlow<Boolean>

    fun setCallState(state: CallState)
    fun setSpeakerphoneOn(on: Boolean)
    fun setMicrophoneEnabled(enabled: Boolean)
}
