package ltd.evilcorp.domain.core.network

import ltd.evilcorp.domain.core.network.save.ToxSaveStatus

interface IToxStarter {
    fun tryLoadTox(password: String?): ToxSaveStatus
    fun stopTox()
    fun startTox(save: ByteArray? = null, password: String? = null): ToxSaveStatus
}
