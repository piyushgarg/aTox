// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox.impl

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.tox.runtime.ToxRuntime
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.IToxCallController

@Singleton
class ToxCallControllerImpl @Inject constructor(
    private val runtime: ToxRuntime,
) : IToxCallController {
    override fun startCall(pk: PublicKey): Boolean = runtime.startCall(pk)
    override fun answerCall(pk: PublicKey): Boolean = runtime.answerCall(pk)
    override fun endCall(pk: PublicKey): Boolean = runtime.endCall(pk)
    override fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int): Boolean =
        runtime.sendAudio(pk, pcm, channels, samplingRate)
}
