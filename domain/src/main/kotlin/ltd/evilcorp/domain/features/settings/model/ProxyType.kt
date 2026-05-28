// SPDX-FileCopyrightText: 2019-2020 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.settings.model

/**
 * Proxy server types supported by Tox Core for network traffic.
 */
enum class ProxyType {
    /** Direct connection to the network (no proxy). */
    None,
    /** HTTP proxy server. */
    HTTP,
    /** SOCKS5 proxy server (e.g. Tor on port 9050). */
    SOCKS5,
}
