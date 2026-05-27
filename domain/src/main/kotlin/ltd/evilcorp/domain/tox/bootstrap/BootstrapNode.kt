// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox.bootstrap

import ltd.evilcorp.domain.model.PublicKey

/**
 * Data of a public DHT node (Bootstrap Node).
 * Used for the initial connection of a client to the distributed P2P Tox network.
 */
data class BootstrapNode(
    /** The IP address or domain name of the node. */
    val address: String,
    /** The UDP/TCP port the node listens on. */
    val port: Int,
    /** The 32-byte native public key of the node for encrypting the connection. */
    val publicKey: PublicKey,
)
