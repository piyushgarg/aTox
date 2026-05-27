// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.enums

import ltd.evilcorp.domain.model.UserStatus

/**
 * Enum representing user presence statuses in Tox.
 */
enum class ToxUserStatus {
    /** Online / Available. */
    NONE,
    /** Away. */
    AWAY,
    /** Busy / Do Not Disturb. */
    BUSY;

    /**
     * Converts the internal Tox status to the domain [UserStatus] model.
     */
    fun toUserStatus(): UserStatus = when (this) {
        NONE -> UserStatus.None
        AWAY -> UserStatus.Away
        BUSY -> UserStatus.Busy
    }

    companion object {
        /**
         * Returns the presence status by its JNI integer value.
         * @param value The JNI integer value.
         * @return The corresponding [ToxUserStatus].
         */
        fun fromInt(value: Int): ToxUserStatus = entries.getOrElse(value) { NONE }
    }
}
