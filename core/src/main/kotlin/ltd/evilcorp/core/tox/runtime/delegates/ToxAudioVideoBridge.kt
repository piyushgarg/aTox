package ltd.evilcorp.core.tox.runtime.delegates

import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.core.tox.NativeToxAv
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.enums.ToxavCallControl

class ToxAudioVideoBridge(
    private val nativeTox: NativeTox,
    private val nativeToxAv: NativeToxAv,
    lock: Any,
    toxPtrProvider: () -> Long,
    private val toxavPtrProvider: () -> Long,
) : BaseToxBridge(lock, toxPtrProvider) {
    private fun contactByKey(ptr: Long, pk: PublicKey): Int {
        return nativeTox.toxFriendByPublicKey(ptr, pk.bytes())
    }

    private inline fun <T> withToxAv(block: (Long, Long) -> T): T = synchronized(lock) {
        val ptr = toxPtrProvider()
        val avPtr = toxavPtrProvider()
        check(ptr != 0L) { "Tox native pointer is null." }
        check(avPtr != 0L) { "Toxav native pointer is null." }
        block(ptr, avPtr)
    }

    fun startCall(pk: PublicKey, audioBitrate: Int) = withToxAv { ptr, avPtr ->
        nativeToxAv.toxavCall(avPtr, contactByKey(ptr, pk), audioBitrate, 0)
    }

    fun answerCall(pk: PublicKey, audioBitrate: Int) = withToxAv { ptr, avPtr ->
        nativeToxAv.toxavAnswer(avPtr, contactByKey(ptr, pk), audioBitrate, 0)
    }

    fun endCall(pk: PublicKey) = withToxAv { ptr, avPtr ->
        nativeToxAv.toxavCallControl(avPtr, contactByKey(ptr, pk), ToxavCallControl.CANCEL.ordinal)
    }

    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) = withToxAv { ptr, avPtr ->
        nativeToxAv.toxavAudioSendFrame(avPtr, contactByKey(ptr, pk), pcm, pcm.size, channels, samplingRate)
    }

    fun sendVideoFrame(pk: PublicKey, width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray): Boolean = withToxAv { ptr, avPtr ->
        nativeToxAv.toxavVideoSendFrame(avPtr, contactByKey(ptr, pk), width, height, y, u, v)
    }

    fun audioSetBitRate(pk: PublicKey, bitrate: Int): Boolean = withToxAv { ptr, avPtr ->
        nativeToxAv.toxavAudioSetBitRate(avPtr, contactByKey(ptr, pk), bitrate)
    }

    fun videoSetBitRate(pk: PublicKey, bitrate: Int): Boolean = withToxAv { ptr, avPtr ->
        nativeToxAv.toxavVideoSetBitRate(avPtr, contactByKey(ptr, pk), bitrate)
    }
}
