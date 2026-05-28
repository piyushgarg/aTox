package ltd.evilcorp.domain.features.call

interface IAudioRoutingManager {
    fun requestCallAudioFocus(onFocusLoss: () -> Unit, onFocusGain: () -> Unit): Boolean
    fun abandonCallAudioFocus()
    fun setSpeakerphoneRoute(on: Boolean)
    fun setCommunicationMode(active: Boolean)
}
