package ltd.evilcorp.domain.core.network

interface ITimeProvider {
    fun getCurrentTimeMillis(): Long
}
