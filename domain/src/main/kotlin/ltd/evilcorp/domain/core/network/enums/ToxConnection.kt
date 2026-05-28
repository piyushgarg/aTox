// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.enums

import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus

/**
 * Enum representing Tox DHT network connection types.
 */
enum class ToxConnection {
    /** No connection. */
    NONE,
    /** Connection via TCP protocol (through TCP relays). */
    TCP,
    /** Direct UDP connection with DHT nodes. */
    UDP;

    /**
     * Converts the internal Tox connection type to the domain [ConnectionStatus] model.
     */
    fun toConnectionStatus(): ConnectionStatus = when (this) {
        NONE -> ConnectionStatus.None
        TCP -> ConnectionStatus.TCP
        UDP -> ConnectionStatus.UDP
    }

    companion object {
        /**
         * Returns the enum element by its integer value.
         * @param value The integer value corresponding to the connection type.
         * @return The corresponding [ToxConnection].
         */
        fun fromInt(value: Int): ToxConnection = entries.getOrElse(value) { NONE }
    }
}
