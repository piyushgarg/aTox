package ltd.evilcorp.core.platform.system

import ltd.evilcorp.domain.core.network.ITimeProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimeProviderImpl @Inject constructor() : ITimeProvider {
    override fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
