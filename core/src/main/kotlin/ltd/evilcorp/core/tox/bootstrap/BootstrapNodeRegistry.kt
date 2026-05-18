package ltd.evilcorp.core.tox.bootstrap

interface BootstrapNodeRegistry {
    fun get(n: Int): List<BootstrapNode>
    fun reset()
}
