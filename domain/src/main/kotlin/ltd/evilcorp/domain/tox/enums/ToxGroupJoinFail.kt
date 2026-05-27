// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.enums

/**
 * Enum representing the reasons for failing to join an NGC group chat.
 * Matches `Tox_Group_Join_Fail` codes in toxcore.
 */
enum class ToxGroupJoinFail(val value: Int) {
    /** The conference peer limit has been exceeded. */
    PEER_LIMIT(0),
    /** The password provided for joining is incorrect. */
    INVALID_PASSWORD(1),
    /** Unknown connection error. */
    UNKNOWN(2);

    companion object {
        /**
         * Returns the join fail reason by its JNI integer value.
         * @param value The JNI integer value.
         * @return The corresponding [ToxGroupJoinFail].
         */
        fun fromInt(value: Int): ToxGroupJoinFail =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
