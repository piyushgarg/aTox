// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.bootstrap

/**
 * Interface representing a registry of bootstrap servers (DHT nodes).
 * Responsible for providing nodes for the initial P2P connection.
 */
interface BootstrapNodeRegistry {
    /**
     * Returns a randomized list of [n] bootstrap nodes.
     * @param n The required number of nodes.
     */
    fun get(n: Int): List<BootstrapNode>

    /**
     * Resets the registry state and reloads the cached nodes.
     */
    fun reset()
}
