// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.enums

/**
 * Enum representing the privacy modes of an NGC group chat (Next Generation Conferences).
 * Matches `Tox_Group_Privacy_State` codes in toxcore.
 */
enum class ToxGroupPrivacyState(val value: Int) {
    /** Public chat (anyone can join without a password). */
    PUBLIC(0),
    /** Private chat (invitation or password required). */
    PRIVATE(1);

    companion object {
        /**
         * Returns the privacy state by its JNI integer value.
         * @param value The JNI integer value.
         * @return The corresponding [ToxGroupPrivacyState].
         */
        fun fromInt(value: Int): ToxGroupPrivacyState =
            entries.firstOrNull { it.value == value } ?: PUBLIC
    }
}
