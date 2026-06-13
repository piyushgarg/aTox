// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.bootstrap

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard implementation of [IBootstrapNodeRegistry] for managing bootstrap nodes.
 * Loads a list of nodes from [IBootstrapNodeJsonSource], parses it via [BootstrapNodeJsonParser],
 * and provides a shuffled selection of nodes for P2P connection.
 */
@Singleton
class DefaultBootstrapNodeRegistry @Inject constructor(
    private val parser: BootstrapNodeJsonParser,
    private val source: IBootstrapNodeJsonSource,
    @ltd.evilcorp.domain.core.di.IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
) : IBootstrapNodeRegistry {
    private val mutex = Mutex()
    private var nodes: List<BootstrapNode> = emptyList()

    /**
     * Provides a randomized selection of [n] bootstrap nodes.
     */
    override suspend fun get(n: Int): List<BootstrapNode> = withContext(ioDispatcher) {
        mutex.withLock {
            if (nodes.isEmpty()) {
                reloadNodes()
            }
            nodes.asSequence().shuffled().take(n).toList()
        }
    }

    /**
     * Resets the registry state and reloads the cached nodes from the source.
     */
    override suspend fun reset() = mutex.withLock {
        nodes = emptyList()
    }

    private fun reloadNodes() {
        nodes = source.load()?.let(parser::parse).orEmpty()
    }
}
