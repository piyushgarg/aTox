package ltd.evilcorp.domain.feature

interface TimeProvider {
    fun getCurrentTimeMillis(): Long
}
