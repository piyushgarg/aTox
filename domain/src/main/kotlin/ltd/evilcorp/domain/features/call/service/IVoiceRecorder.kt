package ltd.evilcorp.domain.features.call.service

interface IVoiceRecorder {
    fun startRecording(): Boolean
    suspend fun stopRecording(): String?
    suspend fun cancelRecording()
    fun release()
}
