package ltd.evilcorp.core.tox.runtime.delegates

abstract class BaseToxBridge(
    protected val lock: Any,
    protected val toxPtrProvider: () -> Long,
) {
    protected inline fun <T> withTox(block: (Long) -> T): T = synchronized(lock) {
        val ptr = toxPtrProvider()
        check(ptr != 0L) { "Tox native pointer is null. Tox session might have stopped or is not initialized." }
        block(ptr)
    }
}
