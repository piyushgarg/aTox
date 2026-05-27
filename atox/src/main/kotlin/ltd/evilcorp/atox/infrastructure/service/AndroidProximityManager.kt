package ltd.evilcorp.atox.infrastructure.service

import ltd.evilcorp.domain.feature.ProximityManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidProximityManager @Inject constructor(
    private val proximityScreenOff: ProximityScreenOff
) : ProximityManager {
    override fun acquire() = proximityScreenOff.acquire()
    override fun release() = proximityScreenOff.release()
}
