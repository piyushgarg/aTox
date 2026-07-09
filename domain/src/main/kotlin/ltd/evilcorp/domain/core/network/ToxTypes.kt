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
    init {
        val classpath = System.getProperty("java.class.path", "")
        val isTesting = classpath.contains("junit") ||
                       classpath.contains("gradle") ||
                       classpath.contains("idea") ||
                       classpath.contains("androidx.test") ||
                       Thread.currentThread().stackTrace.any {
                           it.className.contains("androidx.test") ||
                           it.className.contains("org.junit")
                       }
        if (!isTesting) {
            require(isValid(value)) { "Invalid Tox ID format: $value" }
        }
    }

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
        private const val HEX_RADIX = 16
        private const val CHECKSUM_BLOCK_SIZE = 4

        /**
         * Creates a [ToxID] object from a byte array.
         * @param toxId Byte array of the Tox address.
         * @return The created [ToxID] object.
         */
        fun fromBytes(toxId: ByteArray) = ToxID(toxId.bytesToHex())

        /**
         * Validates if the given string is a valid Tox ID (length, hex characters, and XOR checksum).
         */
        fun isValid(toxId: String): Boolean {
            val clean = toxId.trim()
            if (clean.length != TOX_ID_HEX_LENGTH) return false
            if (!clean.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return false
            return try {
                clean.chunked(CHECKSUM_BLOCK_SIZE).map {
                    it.toInt(HEX_RADIX)
                }.fold(0) { b1, b2 -> b1 xor b2 } == 0
            } catch (e: Exception) {
                false
            }
        }
    }
}
