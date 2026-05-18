// SPDX-FileCopyrightText: 2019-2024 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import ltd.evilcorp.core.tox.enums.ToxConnection
import ltd.evilcorp.core.tox.enums.ToxFileKind
import ltd.evilcorp.core.tox.enums.ToxMessageType
import ltd.evilcorp.core.tox.enums.ToxUserStatus
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.FileKind
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.UserStatus



fun String.hexToBytes(): ByteArray = chunked(2).map { it.uppercase().toInt(radix = 16).toByte() }.toByteArray()
fun ByteArray.bytesToHex(): String = this.joinToString("") { "%02X".format(it) }


fun UserStatus.toToxType(): ToxUserStatus = when (this) {
    UserStatus.None -> ToxUserStatus.NONE
    UserStatus.Away -> ToxUserStatus.AWAY
    UserStatus.Busy -> ToxUserStatus.BUSY
}

fun MessageType.toToxType(): ToxMessageType = when (this) {
    MessageType.Normal -> ToxMessageType.NORMAL
    MessageType.Action -> ToxMessageType.ACTION
    MessageType.FileTransfer -> throw Exception("File transfer message type doesn't exist in Tox")
}

fun FileKind.toToxtype(): Int = when (this) {
    FileKind.Avatar -> ToxFileKind.AVATAR.ordinal
    FileKind.Data -> ToxFileKind.DATA.ordinal
}
