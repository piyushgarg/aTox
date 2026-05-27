// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.enums

/**
 * Enum representing the lock modes for changing the topic in an NGC group chat.
 * Matches `Tox_Group_Topic_Lock` codes in toxcore.
 */
enum class ToxGroupTopicLock(val value: Int) {
    /** Topic modification is locked (only moderators and the founder can change the topic). */
    ENABLED(0),
    /** Topic modification is allowed for all participants in the conference. */
    DISABLED(1);

    companion object {
        /**
         * Returns the topic lock mode by its JNI integer value.
         * @param value The JNI integer value.
         * @return The corresponding [ToxGroupTopicLock].
         */
        fun fromInt(value: Int): ToxGroupTopicLock =
            entries.firstOrNull { it.value == value } ?: ENABLED
    }
}
