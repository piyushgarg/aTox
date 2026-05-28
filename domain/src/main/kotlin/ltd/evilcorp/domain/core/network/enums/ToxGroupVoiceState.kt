// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.enums

/**
 * Enum representing voice broadcasting states (voice privileges) in an NGC group call.
 * Matches `Tox_Group_Voice_State` codes in toxcore.
 */
enum class ToxGroupVoiceState(val value: Int) {
    /** Voice broadcasting is allowed for all participants in the group. */
    ALL(0),
    /** Only moderators and the founder can speak. */
    MODERATOR(1),
    /** Voice broadcasting is allowed only for the group founder. */
    FOUNDER(2);

    companion object {
        /**
         * Returns the voice state by its JNI integer value.
         * @param value The JNI integer value.
         * @return The corresponding [ToxGroupVoiceState].
         */
        fun fromInt(value: Int): ToxGroupVoiceState =
            entries.firstOrNull { it.value == value } ?: ALL
    }
}
