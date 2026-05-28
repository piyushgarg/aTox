package ltd.evilcorp.atox.infrastructure.service

import ltd.evilcorp.domain.features.call.IProximityManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidProximityManager @Inject constructor(
    private val proximityScreenOff: ProximityScreenOff
) : IProximityManager {
    override fun acquire() = proximityScreenOff.acquire()
    override fun release() = proximityScreenOff.release()
}
