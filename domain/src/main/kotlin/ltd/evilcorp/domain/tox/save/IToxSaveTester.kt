// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.save

import ltd.evilcorp.domain.model.ProxyStatus
import ltd.evilcorp.domain.model.ProxyType

interface IToxSaveTester {
    fun testProxy(
        udpEnabled: Boolean,
        proxyType: ProxyType,
        proxyAddress: String,
        proxyPort: Int,
    ): ProxyStatus
}
