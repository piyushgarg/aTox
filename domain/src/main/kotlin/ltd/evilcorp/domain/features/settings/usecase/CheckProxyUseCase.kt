// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.settings.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.domain.features.settings.model.ProxyStatus
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.core.network.save.IToxSaveTester

class CheckProxyUseCase @Inject constructor(
    private val toxSaveTester: IToxSaveTester,
) {
    suspend fun execute(
        udpEnabled: Boolean,
        proxyType: ProxyType,
        proxyAddress: String,
        proxyPort: Int,
    ): ProxyStatus = withContext(Dispatchers.IO) {
        toxSaveTester.testProxy(udpEnabled, proxyType, proxyAddress, proxyPort)
    }
}
