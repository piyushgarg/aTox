package ltd.evilcorp.domain.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import ltd.evilcorp.domain.features.call.CallState
import ltd.evilcorp.domain.features.call.ICallSessionRegistry

class FakeCallSessionRegistry : ICallSessionRegistry {
    override val inCall = MutableStateFlow<CallState>(CallState.Idle)
    override val speakerphoneOn = MutableStateFlow<Boolean>(false)
    override val microphoneEnabled = MutableStateFlow<Boolean>(true)

    override fun setCallState(state: CallState) {
        inCall.value = state
    }

    override fun setSpeakerphoneOn(on: Boolean) {
        speakerphoneOn.value = on
    }

    override fun setMicrophoneEnabled(enabled: Boolean) {
        microphoneEnabled.value = enabled
    }
}
