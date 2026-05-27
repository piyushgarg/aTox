// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.bootstrap

/**
 * Interface representing a source of a static JSON list of Tox bootstrap servers.
 */
interface BootstrapNodeJsonSource {
    /**
     * Loads the JSON string containing the list of servers.
     * @return The JSON text, or null if the load failed.
     */
    fun load(): String?
}
