// SPDX-FileCopyrightText: 2020-2021 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox.save

import ltd.evilcorp.core.tox.NativeTox

enum class ToxSaveStatus {
    Ok,
    BadFormat,
    Encrypted,
    OutOfMemory,
    Null,
    PortAlloc,
    BadProxyHost,
    BadProxyPort,
    BadProxyType,
    ProxyNotFound,
    SaveNotFound,
}

fun testToxSave(options: SaveOptions, password: String?): ToxSaveStatus {
    val native = NativeTox()
    return try {
        val rawSaveData = options.saveData
        val saveData = if (password != null && rawSaveData != null) {
            val salt = native.getSalt(rawSaveData) ?: return ToxSaveStatus.BadFormat
            val passkey = native.passKeyDeriveWithSalt(password.toByteArray(), salt)
                ?: return ToxSaveStatus.OutOfMemory
            native.passDecrypt(rawSaveData, passkey) ?: return ToxSaveStatus.Encrypted
        } else {
            rawSaveData
        }
        val toxPtr = native.toxNew(saveData)
        if (toxPtr == 0L) {
            return ToxSaveStatus.BadFormat
        }
        native.toxKill(toxPtr)
        ToxSaveStatus.Ok
    } catch (e: Exception) {
        ToxSaveStatus.BadFormat
    }
}
