// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import ltd.evilcorp.core.model.PublicKey

private const val ID_METADATA_LEN = 12

@JvmInline
value class ToxID(private val value: String) {
    fun bytes() = value.hexToBytes()
    fun string() = value

    fun toPublicKey() = PublicKey(value.dropLast(ID_METADATA_LEN))

    companion object {
        fun fromBytes(toxId: ByteArray) = ToxID(toxId.bytesToHex())
    }
}
