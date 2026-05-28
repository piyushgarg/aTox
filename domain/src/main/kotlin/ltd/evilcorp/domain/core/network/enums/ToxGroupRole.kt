// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.enums

/**
 * Enum representing peer roles (privileges) in an NGC group chat.
 * Matches `Tox_Group_Role` codes in toxcore.
 */
enum class ToxGroupRole(val value: Int) {
    /** Founder of the group (full control over permissions and passwords). */
    FOUNDER(0),
    /** Moderator of the chat (can kick and regulate peer statuses/voice permissions). */
    MODERATOR(1),
    /** Standard User (can send text messages/audio). */
    USER(2),
    /** Observer (cannot send text messages or speak). */
    OBSERVER(3);

    companion object {
        /**
         * Returns the peer role by its JNI integer value.
         * @param value The JNI integer value.
         * @return The corresponding [ToxGroupRole].
         */
        fun fromInt(value: Int): ToxGroupRole =
            entries.firstOrNull { it.value == value } ?: USER
    }
}
