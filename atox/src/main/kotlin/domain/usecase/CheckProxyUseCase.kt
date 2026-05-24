package ltd.evilcorp.atox.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.core.tox.save.ProxyType
import ltd.evilcorp.core.tox.save.SaveOptions
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import ltd.evilcorp.core.tox.save.testToxSave

enum class ProxyStatus {
    Good,
    BadPort,
    BadHost,
    BadType,
    NotFound,
}

class CheckProxyUseCase @Inject constructor() {
    suspend fun execute(
        udpEnabled: Boolean,
        proxyType: ProxyType,
        proxyAddress: String,
        proxyPort: Int,
    ): ProxyStatus = withContext(Dispatchers.IO) {
        val saveStatus = testToxSave(
            SaveOptions(saveData = null, udpEnabled, proxyType, proxyAddress, proxyPort),
            null,
        )

        when (saveStatus) {
            ToxSaveStatus.BadProxyHost -> ProxyStatus.BadHost
            ToxSaveStatus.BadProxyPort -> ProxyStatus.BadPort
            ToxSaveStatus.BadProxyType -> ProxyStatus.BadType
            ToxSaveStatus.ProxyNotFound -> ProxyStatus.NotFound
            else -> ProxyStatus.Good
        }
    }
}
