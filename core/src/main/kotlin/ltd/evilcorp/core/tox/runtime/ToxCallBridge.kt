package ltd.evilcorp.core.tox.runtime

import ltd.evilcorp.domain.model.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToxCallBridge @Inject constructor() {
    private var _toxWrapper: ToxWrapper? = null

    fun init(toxWrapper: ToxWrapper) {
        _toxWrapper = toxWrapper
    }

    private fun wrapper(): ToxWrapper = _toxWrapper ?: error("ToxCallBridge not initialized with ToxWrapper")

    fun startCall(pk: PublicKey) = wrapper().startCall(pk)
    fun answerCall(pk: PublicKey) = wrapper().answerCall(pk)
    fun endCall(pk: PublicKey) = wrapper().endCall(pk)

    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) =
        wrapper().sendAudio(pk, pcm, channels, samplingRate)

    fun sendVideoFrame(pk: PublicKey, width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray): Boolean =
        wrapper().sendVideoFrame(pk, width, height, y, u, v)

    fun audioSetBitRate(pk: PublicKey, bitrate: Int): Boolean =
        wrapper().audioSetBitRate(pk, bitrate)

    fun videoSetBitRate(pk: PublicKey, bitrate: Int): Boolean =
        wrapper().videoSetBitRate(pk, bitrate)
}
