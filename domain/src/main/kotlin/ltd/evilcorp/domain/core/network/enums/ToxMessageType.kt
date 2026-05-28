// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.enums

import ltd.evilcorp.domain.features.chat.model.MessageType

/**
 * Enum representing message types in the Tox protocol.
 */
enum class ToxMessageType {
    /** Standard text message. */
    NORMAL,
    /** Action message (slash command, e.g., "/me is sleeping"). */
    ACTION;

    /**
     * Converts the internal Tox message type to the domain [MessageType] model.
     */
    fun toMessageType(): MessageType = when (this) {
        NORMAL -> MessageType.Normal
        ACTION -> MessageType.Action
    }

    companion object {
        /**
         * Returns the message type by its JNI integer value.
         * @param value The JNI integer value.
         * @return The corresponding [ToxMessageType].
         */
        fun fromInt(value: Int): ToxMessageType = entries.getOrElse(value) { NORMAL }
    }
}
