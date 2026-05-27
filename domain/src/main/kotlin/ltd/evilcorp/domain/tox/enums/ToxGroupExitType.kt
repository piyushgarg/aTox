// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.enums

/**
 * Enum representing the reasons for a peer leaving or being kicked from an NGC group chat.
 * Matches `Tox_Group_Exit_Type` codes in toxcore.
 */
enum class ToxGroupExitType(val value: Int) {
    /** The participant voluntarily left the chat. */
    QUIT(0),
    /** The connection timed out. */
    TIMEOUT(1),
    /** The participant disconnected from the P2P network. */
    DISCONNECTED(2),
    /** Self-disconnection from the group conference. */
    SELF_DISCONNECTED(3),
    /** The participant was kicked by a moderator. */
    KICK(4),
    /** Chat state data synchronization error. */
    SYNC_ERROR(5);

    companion object {
        /**
         * Returns the exit type by its JNI integer value.
         * @param value The JNI integer value.
         * @return The corresponding [ToxGroupExitType].
         */
        fun fromInt(value: Int): ToxGroupExitType =
            entries.firstOrNull { it.value == value } ?: QUIT
    }
}
