package ltd.evilcorp.core.platform.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ltd.evilcorp.domain.features.call.CallState
import ltd.evilcorp.domain.features.call.ICallSessionRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallSessionRegistryImpl @Inject constructor() : ICallSessionRegistry {
    private val _inCall = MutableStateFlow<CallState>(CallState.Idle)
    override val inCall: StateFlow<CallState> = _inCall.asStateFlow()

    private val _speakerphoneOn = MutableStateFlow(false)
    override val speakerphoneOn: StateFlow<Boolean> = _speakerphoneOn.asStateFlow()

    private val _microphoneEnabled = MutableStateFlow(true)
    override val microphoneEnabled: StateFlow<Boolean> = _microphoneEnabled.asStateFlow()

    override fun setCallState(state: CallState) {
        _inCall.value = state
    }

    override fun setSpeakerphoneOn(on: Boolean) {
        _speakerphoneOn.value = on
    }

    override fun setMicrophoneEnabled(enabled: Boolean) {
        _microphoneEnabled.value = enabled
    }
}
