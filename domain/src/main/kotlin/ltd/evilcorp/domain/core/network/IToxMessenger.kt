// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.model.MessageType

interface IToxMessenger {
    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int
    fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean
    fun friendGetTyping(publicKey: PublicKey): Boolean
    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean
}
