// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox.impl

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.tox.runtime.ToxRuntime
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.IToxMessenger
import ltd.evilcorp.domain.features.chat.model.MessageType

@Singleton
class ToxMessengerImpl @Inject constructor(
    private val runtime: ToxRuntime,
) : IToxMessenger {
    override fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int =
        runtime.sendMessage(publicKey, message, type)

    override fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean {
        runtime.setTyping(publicKey, typing)
        return true
    }

    override fun friendGetTyping(publicKey: PublicKey): Boolean = runtime.friendGetTyping(publicKey)

    override fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean {
        runtime.sendLosslessPacket(pk, packet)
        return true
    }
}
