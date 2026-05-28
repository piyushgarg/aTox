// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.enums

/**
 * Enum representing moderation events (administrative actions) in an NGC group chat.
 * Matches `Tox_Group_Mod_Event` codes in toxcore.
 */
enum class ToxGroupModEvent(val value: Int) {
    /** The participant was kicked from the group. */
    KICK(0),
    /** The participant status changed to Observer (cannot voice/text). */
    OBSERVER(1),
    /** The participant status changed to User (basic permissions). */
    USER(2),
    /** The participant was promoted to Moderator. */
    MODERATOR(3);

    companion object {
        /**
         * Returns the moderation event type by its JNI integer value.
         * @param value The JNI integer value.
         * @return The corresponding [ToxGroupModEvent].
         */
        fun fromInt(value: Int): ToxGroupModEvent =
            entries.firstOrNull { it.value == value } ?: USER
    }
}
