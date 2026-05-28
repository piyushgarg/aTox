// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox.save

import javax.inject.Inject
import ltd.evilcorp.domain.features.settings.model.ProxyStatus
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.core.network.save.IToxSaveTester
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus

class ToxSaveTesterImpl @Inject constructor() : IToxSaveTester {
    override fun testProxy(
        udpEnabled: Boolean,
        proxyType: ProxyType,
        proxyAddress: String,
        proxyPort: Int,
    ): ProxyStatus {
        val options = SaveOptions(
            saveData = null,
            udpEnabled = udpEnabled,
            proxyType = proxyType,
            proxyAddress = proxyAddress,
            proxyPort = proxyPort
        )
        val saveStatus = testToxSave(options, null)
        return when (saveStatus) {
            ToxSaveStatus.BadProxyHost -> ProxyStatus.BadHost
            ToxSaveStatus.BadProxyPort -> ProxyStatus.BadPort
            ToxSaveStatus.BadProxyType -> ProxyStatus.BadType
            ToxSaveStatus.ProxyNotFound -> ProxyStatus.NotFound
            else -> ProxyStatus.Good
        }
    }
}
