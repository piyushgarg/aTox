// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.enums

/**
 * Enum representing Tox File Control commands.
 */
enum class ToxFileControl {
    /** Resume or start file transfer. */
    RESUME,
    /** Pause file transfer. */
    PAUSE,
    /** Cancel or reject file transfer. */
    CANCEL;

    companion object {
        /**
         * Returns the file control command by its JNI integer value.
         * @param value The integer value of the command.
         * @return The corresponding [ToxFileControl].
         */
        fun fromInt(value: Int): ToxFileControl = entries.getOrElse(value) { CANCEL }
    }
}
