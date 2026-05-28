// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network

private const val CHECKSUM_BLOCK_SIZE = 4

/**
 * Validator for Tox ID identifiers.
 * Provides static methods to verify the correctness of 76-character HEX strings of Tox addresses.
 */
class ToxIdValidator {
    /**
     * The result of Tox ID validation.
     */
    enum class Result {
        /** Validation passed successfully. */
        NO_ERROR,
        /** Incorrect string length (expected 76 characters). */
        INCORRECT_LENGTH,
        /** Checksum mismatch. */
        INVALID_CHECKSUM,
        /** The string contains non-HEX characters. */
        NOT_HEX,
    }

    companion object {
        /**
         * Validates the provided ToxID.
         * Verifies the HEX format, string length (76 characters), and checksum using a byte-wise XOR algorithm.
         * @param toxID The Tox ID to validate.
         * @return The validation [Result].
         */
        fun validate(toxID: ToxID) = when {
            !toxID.string().matches(Regex("[0-9A-Fa-f]*")) -> Result.NOT_HEX
            toxID.string().length != TOX_ID_LENGTH -> Result.INCORRECT_LENGTH
            toxID.string().chunked(CHECKSUM_BLOCK_SIZE).map {
                it.toInt(radix = 16)
            }.fold(0) { b1, b2 -> b1 xor b2 } != 0 -> Result.INVALID_CHECKSUM
            else -> Result.NO_ERROR
        }
    }
}
