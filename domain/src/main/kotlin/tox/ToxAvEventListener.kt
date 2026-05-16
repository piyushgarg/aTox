// SPDX-FileCopyrightText: 2020-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox

import ltd.evilcorp.domain.tox.enums.ToxavFriendCallState
import java.util.EnumSet
import javax.inject.Inject
import ltd.evilcorp.core.vo.PublicKey

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

class ToxAvEventListener @Inject constructor() {
    var contactMapping: List<Pair<PublicKey, Int>> = listOf()

    var callHandler: CallHandler = { _, _, _ -> }
    var callStateHandler: CallStateHandler = { _, _ -> }
    var videoBitRateHandler: VideoBitRateHandler = { _, _ -> }
    var videoReceiveFrameHandler: VideoReceiveFrameHandler = { _, _, _, _, _, _, _, _, _ -> }
    var audioReceiveFrameHandler: AudioReceiveFrameHandler = { _, _, _, _ -> }
    var audioBitRateHandler: AudioBitRateHandler = { _, _ -> }

    private fun keyFor(friendNo: Int) = contactMapping.find { it.second == friendNo }!!.first.string()

    fun call(friendNo: Int, audioEnabled: Boolean, videoEnabled: Boolean) =
        callHandler(keyFor(friendNo), audioEnabled, videoEnabled)

    fun videoBitRate(friendNo: Int, bitRate: Int) = videoBitRateHandler(keyFor(friendNo), bitRate)

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
    ) = videoReceiveFrameHandler(keyFor(friendNo), width, height, y, u, v, yStride, uStride, vStride)

    fun callState(friendNo: Int, callState: EnumSet<ToxavFriendCallState>) =
        callStateHandler(keyFor(friendNo), callState)

    fun audioReceiveFrame(friendNo: Int, pcm: ShortArray, channels: Int, samplingRate: Int) =
        audioReceiveFrameHandler(keyFor(friendNo), pcm, channels, samplingRate)

    fun audioBitRate(friendNo: Int, bitRate: Int) = audioBitRateHandler(keyFor(friendNo), bitRate)

    // JNI Bridge methods
    fun onCall(friendNo: Int, audioEnabled: Boolean, videoEnabled: Boolean) =
        call(friendNo, audioEnabled, videoEnabled)

    fun onCallState(friendNo: Int, state: Int) {
        // Map Int bitmask to EnumSet
        val set = EnumSet.noneOf(ToxavFriendCallState::class.java)
        if (state and 1 != 0) set.add(ToxavFriendCallState.Error)
        if (state and 2 != 0) set.add(ToxavFriendCallState.Finished)
        if (state and 4 != 0) set.add(ToxavFriendCallState.SendingAudio)
        if (state and 8 != 0) set.add(ToxavFriendCallState.SendingVideo)
        if (state and 16 != 0) set.add(ToxavFriendCallState.ReceivingAudio)
        if (state and 32 != 0) set.add(ToxavFriendCallState.ReceivingVideo)
        callState(friendNo, set)
    }

    fun onAudioReceiveFrame(friendNo: Int, pcm: ShortArray, sampleCount: Int, channels: Int, samplingRate: Int) =
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
