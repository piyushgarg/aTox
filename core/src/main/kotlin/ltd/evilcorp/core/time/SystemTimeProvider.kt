package ltd.evilcorp.core.time

import ltd.evilcorp.domain.feature.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
