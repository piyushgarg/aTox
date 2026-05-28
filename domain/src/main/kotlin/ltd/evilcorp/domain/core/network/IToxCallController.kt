// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network

import ltd.evilcorp.domain.core.model.PublicKey

interface IToxCallController {
    fun startCall(pk: PublicKey): Boolean
    fun answerCall(pk: PublicKey): Boolean
    fun endCall(pk: PublicKey): Boolean
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int): Boolean
}
