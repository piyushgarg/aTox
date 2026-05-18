package ltd.evilcorp.core.tox.bootstrap

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBootstrapNodeRegistry @Inject constructor(
    private val parser: BootstrapNodeJsonParser,
    private val source: BootstrapNodeJsonSource,
) : BootstrapNodeRegistry {
    private var nodes: List<BootstrapNode> = emptyList()

    init {
        reset()
    }

    override fun get(n: Int): List<BootstrapNode> =
        nodes.asSequence().shuffled().take(n).toList()

    override fun reset() {
        nodes = source.load()?.let(parser::parse).orEmpty()
    }
}
