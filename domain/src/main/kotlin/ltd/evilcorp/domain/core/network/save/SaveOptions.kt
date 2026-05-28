// SPDX-FileCopyrightText: 2019-2020 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.save

import ltd.evilcorp.domain.features.settings.model.ProxyType

/**
 * Configuration parameters for initializing a native Tox Core session.
 * Contains binary profile save state and network settings (UDP, proxy).
 */
@Suppress("ArrayInDataClass")
data class SaveOptions(
    /** Binary profile save data. Pass null when creating a new empty profile. */
    val saveData: ByteArray?,
    /** Flag to enable UDP. If false, Tox core operates exclusively in TCP mode. */
    val udpEnabled: Boolean,
    /** Type of the proxy server to use. */
    val proxyType: ProxyType,
    /** Network address of the proxy host (IP or domain name). */
    val proxyAddress: String,
    /** Network port of the proxy server. */
    val proxyPort: Int,
)
