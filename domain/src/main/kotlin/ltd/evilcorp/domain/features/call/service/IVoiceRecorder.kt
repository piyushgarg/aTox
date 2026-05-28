package ltd.evilcorp.domain.features.call.service

interface IVoiceRecorder {
    fun startRecording(): Boolean
    fun stopRecording(): String?
    fun cancelRecording()
    fun release()
}
