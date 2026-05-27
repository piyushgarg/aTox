package ltd.evilcorp.core.tox.runtime.delegates

import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.core.tox.NativeToxAv
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.tox.enums.ToxavCallControl

class ToxAudioVideoBridge(
    private val nativeTox: NativeTox,
    private val nativeToxAv: NativeToxAv,
    private val toxPtrProvider: () -> Long,
    private val toxavPtrProvider: () -> Long,
) {
    private fun contactByKey(pk: PublicKey): Int {
        return nativeTox.toxFriendByPublicKey(toxPtrProvider(), pk.bytes())
    }

    fun startCall(pk: PublicKey, audioBitrate: Int) = synchronized(this) {
        nativeToxAv.toxavCall(toxavPtrProvider(), contactByKey(pk), audioBitrate, 0)
    }

    fun answerCall(pk: PublicKey, audioBitrate: Int) = synchronized(this) {
        nativeToxAv.toxavAnswer(toxavPtrProvider(), contactByKey(pk), audioBitrate, 0)
    }

    fun endCall(pk: PublicKey) = synchronized(this) {
        nativeToxAv.toxavCallControl(toxavPtrProvider(), contactByKey(pk), ToxavCallControl.CANCEL.ordinal)
    }

    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) = synchronized(this) {
        nativeToxAv.toxavAudioSendFrame(toxavPtrProvider(), contactByKey(pk), pcm, pcm.size, channels, samplingRate)
    }

    fun sendVideoFrame(pk: PublicKey, width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray): Boolean = synchronized(this) {
        nativeToxAv.toxavVideoSendFrame(toxavPtrProvider(), contactByKey(pk), width, height, y, u, v)
    }

    fun audioSetBitRate(pk: PublicKey, bitrate: Int): Boolean = synchronized(this) {
        nativeToxAv.toxavAudioSetBitRate(toxavPtrProvider(), contactByKey(pk), bitrate)
    }

    fun videoSetBitRate(pk: PublicKey, bitrate: Int): Boolean = synchronized(this) {
        nativeToxAv.toxavVideoSetBitRate(toxavPtrProvider(), contactByKey(pk), bitrate)
    }
}
