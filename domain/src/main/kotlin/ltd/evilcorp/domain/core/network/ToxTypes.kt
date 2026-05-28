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
        /**
         * Creates a [ToxID] object from a byte array.
         * @param toxId Byte array of the Tox address.
         * @return The created [ToxID] object.
         */
        fun fromBytes(toxId: ByteArray) = ToxID(toxId.bytesToHex())
    }
}
