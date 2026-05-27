// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.bootstrap

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard implementation of [BootstrapNodeRegistry] for managing bootstrap nodes.
 * Loads a list of nodes from [BootstrapNodeJsonSource], parses it via [BootstrapNodeJsonParser],
 * and provides a shuffled selection of nodes for P2P connection.
 */
@Singleton
class DefaultBootstrapNodeRegistry @Inject constructor(
    private val parser: BootstrapNodeJsonParser,
    private val source: BootstrapNodeJsonSource,
) : BootstrapNodeRegistry {
    private var nodes: List<BootstrapNode> = emptyList()

    init {
        reset()
    }

    /**
     * Provides a randomized selection of [n] bootstrap nodes.
     */
    override fun get(n: Int): List<BootstrapNode> =
        nodes.asSequence().shuffled().take(n).toList()

    /**
     * Resets the registry state and reloads the cached nodes from the source.
     */
    override fun reset() {
        nodes = source.load()?.let(parser::parse).orEmpty()
    }
}
