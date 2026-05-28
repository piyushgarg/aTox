// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.save

import ltd.evilcorp.domain.features.settings.model.ProxyStatus
import ltd.evilcorp.domain.features.settings.model.ProxyType

interface IToxSaveTester {
    fun testProxy(
        udpEnabled: Boolean,
        proxyType: ProxyType,
        proxyAddress: String,
        proxyPort: Int,
    ): ProxyStatus
}
