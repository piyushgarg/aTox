// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network

import ltd.evilcorp.domain.core.model.PublicKey

private const val ID_METADATA_LEN = 12

/**
 * An inline value class representing a unique 76-character Tox ID identifier.
 * Consists of a 32-byte user public key, a 4-byte nospam value, and a 2-byte checksum.
 */
@JvmInline
value class ToxID(private val value: String) {
    /**
     * Returns the Tox ID as a byte array.
     */
    fun bytes() = value.hexToBytes()

    /**
     * Returns the HEX string representation of the Tox ID.
     */
    fun string() = value

    /**
     * Extracts the public key (32 bytes) from the full Tox ID, dropping the metadata (nospam and checksum).
     */
    fun toPublicKey() = PublicKey(value.dropLast(ID_METADATA_LEN))

    companion object {
        private const val TOX_ID_HEX_LENGTH = 76
        private const val TOX_ID_BYTES_LENGTH = 38
        private const val TOX_ID_PUBLIC_KEY_LENGTH = 32
        private const val TOX_ID_CHECKSUM_OFFSET_0 = 36
        private const val TOX_ID_CHECKSUM_OFFSET_1 = 37
        private const val HEX_CHARS_PER_BYTE = 2
        private const val HEX_RADIX = 16

        /**
         * Creates a [ToxID] object from a byte array.
         * @param toxId Byte array of the Tox address.
         * @return The created [ToxID] object.
         */
        fun fromBytes(toxId: ByteArray) = ToxID(toxId.bytesToHex())

        /**
         * Validates if the given string is a valid Tox ID (length, hex characters, and SHA-256 checksum).
         */
        fun isValid(toxId: String): Boolean {
            val clean = toxId.trim()
            if (clean.length != TOX_ID_HEX_LENGTH) return false
            if (!clean.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return false
            return try {
                val bytes = clean.chunked(HEX_CHARS_PER_BYTE).map { it.toInt(HEX_RADIX).toByte() }.toByteArray()
                if (bytes.size != TOX_ID_BYTES_LENGTH) return false
                val message = bytes.copyOfRange(0, TOX_ID_PUBLIC_KEY_LENGTH)
                val digest = java.security.MessageDigest.getInstance("SHA-256").digest(message)
                bytes[TOX_ID_CHECKSUM_OFFSET_0] == digest[0] && bytes[TOX_ID_CHECKSUM_OFFSET_1] == digest[1]
            } catch (e: Exception) {
                false
            }
        }
    }
}
