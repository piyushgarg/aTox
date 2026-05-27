// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox

import ltd.evilcorp.domain.tox.enums.ToxConnection
import ltd.evilcorp.domain.tox.enums.ToxFileKind
import ltd.evilcorp.domain.tox.enums.ToxMessageType
import ltd.evilcorp.domain.tox.enums.ToxUserStatus
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.FileKind
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.UserStatus

/**
 * Converts a HEX string representation into a byte array.
 * Every two characters of the string are converted into one byte.
 * @return The [ByteArray] representation.
 */
fun String.hexToBytes(): ByteArray = chunked(2).map { it.uppercase().toInt(radix = 16).toByte() }.toByteArray()

/**
 * Converts a byte array into an uppercase HEX string representation.
 * Each byte is formatted as a two-digit hexadecimal number.
 * @return The HEX [String] representation.
 */
fun ByteArray.bytesToHex(): String = this.joinToString("") { "%02X".format(it) }

/**
 * Converts the domain [UserStatus] model to the internal [ToxUserStatus] enum type.
 */
fun UserStatus.toToxType(): ToxUserStatus = when (this) {
    UserStatus.None -> ToxUserStatus.NONE
    UserStatus.Away -> ToxUserStatus.AWAY
    UserStatus.Busy -> ToxUserStatus.BUSY
}

/**
 * Converts the domain [MessageType] model to the internal [ToxMessageType] enum type.
 */
fun MessageType.toToxType(): ToxMessageType = when (this) {
    MessageType.Normal -> ToxMessageType.NORMAL
    MessageType.Action -> ToxMessageType.ACTION
    MessageType.FileTransfer -> throw Exception("MessageType FileTransfer is not supported directly in the Tox protocol")
    MessageType.GroupEvent -> ToxMessageType.NORMAL
}

/**
 * Converts the domain [FileKind] model to the integer representation of [ToxFileKind] for the JNI layer.
 */
fun FileKind.toToxtype(): Int = when (this) {
    FileKind.Avatar -> ToxFileKind.AVATAR.ordinal
    FileKind.Data -> ToxFileKind.DATA.ordinal
}
