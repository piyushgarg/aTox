package ltd.evilcorp.domain.tox

import ltd.evilcorp.domain.tox.save.ToxSaveStatus

interface IToxStarter {
    fun tryLoadTox(password: String?): ToxSaveStatus
    fun stopTox()
    fun startTox(save: ByteArray? = null, password: String? = null): ToxSaveStatus
}
