package ltd.evilcorp.core.tox.listener

import ltd.evilcorp.core.tox.enums.ToxavFriendCallState
import java.util.EnumSet
import javax.inject.Inject
import ltd.evilcorp.core.model.PublicKey

typealias CallHandler = (pk: String, audioEnabled: Boolean, videoEnabled: Boolean) -> Unit
typealias CallStateHandler = (pk: String, callState: EnumSet<ToxavFriendCallState>) -> Unit
typealias VideoBitRateHandler = (pk: String, bitRate: Int) -> Unit
typealias VideoReceiveFrameHandler = (
    pk: String,
    width: Int,
    height: Int,
    y: ByteArray,
    u: ByteArray,
    v: ByteArray,
    yStride: Int,
    uStride: Int,
    vStride: Int,
) -> Unit

typealias AudioReceiveFrameHandler = (pk: String, pcm: ShortArray, channels: Int, samplingRate: Int) -> Unit
typealias AudioBitRateHandler = (pk: String, bitRate: Int) -> Unit

private const val CALL_STATE_ERROR = 1
private const val CALL_STATE_FINISHED = 2
private const val CALL_STATE_SENDING_AUDIO = 4
private const val CALL_STATE_SENDING_VIDEO = 8
private const val CALL_STATE_RECEIVING_AUDIO = 16
private const val CALL_STATE_RECEIVING_VIDEO = 32

class ToxAvEventListener @Inject constructor() {
    var contactMapping: List<Pair<PublicKey, Int>> = listOf()

    var callHandler: CallHandler = { _, _, _ -> }
    var callStateHandler: CallStateHandler = { _, _ -> }
    var videoBitRateHandler: VideoBitRateHandler = { _, _ -> }
    var videoReceiveFrameHandler: VideoReceiveFrameHandler = { _, _, _, _, _, _, _, _, _ -> }
    var audioReceiveFrameHandler: AudioReceiveFrameHandler = { _, _, _, _ -> }
    var audioBitRateHandler: AudioBitRateHandler = { _, _ -> }

    private fun keyFor(friendNo: Int): String? = contactMapping.find { it.second == friendNo }?.first?.string()

    fun call(friendNo: Int, audioEnabled: Boolean, videoEnabled: Boolean) {
        val key = keyFor(friendNo) ?: return
        callHandler(key, audioEnabled, videoEnabled)
    }

    fun videoBitRate(friendNo: Int, bitRate: Int) {
        val key = keyFor(friendNo) ?: return
        videoBitRateHandler(key, bitRate)
    }

    fun videoReceiveFrame(
        friendNo: Int,
        width: Int,
        height: Int,
        y: ByteArray,
        u: ByteArray,
        v: ByteArray,
        yStride: Int,
        uStride: Int,
        vStride: Int,
    ) {
        val key = keyFor(friendNo) ?: return
        videoReceiveFrameHandler(key, width, height, y, u, v, yStride, uStride, vStride)
    }

    fun callState(friendNo: Int, callState: EnumSet<ToxavFriendCallState>) {
        val key = keyFor(friendNo) ?: return
        callStateHandler(key, callState)
    }

    fun audioReceiveFrame(friendNo: Int, pcm: ShortArray, channels: Int, samplingRate: Int) {
        val key = keyFor(friendNo) ?: return
        audioReceiveFrameHandler(key, pcm, channels, samplingRate)
    }

    fun audioBitRate(friendNo: Int, bitRate: Int) {
        val key = keyFor(friendNo) ?: return
        audioBitRateHandler(key, bitRate)
    }

    // JNI Bridge methods
    fun onCall(friendNo: Int, audioEnabled: Boolean, videoEnabled: Boolean) =
        call(friendNo, audioEnabled, videoEnabled)

    fun onCallState(friendNo: Int, state: Int) {
        // Map Int bitmask to EnumSet
        val set = EnumSet.noneOf(ToxavFriendCallState::class.java)
        if (state and CALL_STATE_ERROR != 0) set.add(ToxavFriendCallState.Error)
        if (state and CALL_STATE_FINISHED != 0) set.add(ToxavFriendCallState.Finished)
        if (state and CALL_STATE_SENDING_AUDIO != 0) set.add(ToxavFriendCallState.SendingAudio)
        if (state and CALL_STATE_SENDING_VIDEO != 0) set.add(ToxavFriendCallState.SendingVideo)
        if (state and CALL_STATE_RECEIVING_AUDIO != 0) set.add(ToxavFriendCallState.ReceivingAudio)
        if (state and CALL_STATE_RECEIVING_VIDEO != 0) set.add(ToxavFriendCallState.ReceivingVideo)
        callState(friendNo, set)
    }

    fun onAudioReceiveFrame(friendNo: Int, pcm: ShortArray, @Suppress("UNUSED_PARAMETER") sampleCount: Int, channels: Int, samplingRate: Int) =
        audioReceiveFrame(friendNo, pcm, channels, samplingRate)

    fun onVideoReceiveFrame(
        friendNo: Int,
        width: Int,
        height: Int,
        y: ByteArray,
        u: ByteArray,
        v: ByteArray,
        yStride: Int,
        uStride: Int,
        vStride: Int,
    ) = videoReceiveFrame(friendNo, width, height, y, u, v, yStride, uStride, vStride)

    fun onAudioBitRate(friendNo: Int, bitRate: Int) = audioBitRate(friendNo, bitRate)

    fun onVideoBitRate(friendNo: Int, bitRate: Int) = videoBitRate(friendNo, bitRate)
}
