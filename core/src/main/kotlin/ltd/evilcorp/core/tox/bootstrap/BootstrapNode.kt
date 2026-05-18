package ltd.evilcorp.core.tox.bootstrap

import ltd.evilcorp.core.model.PublicKey

data class BootstrapNode(
    val address: String,
    val port: Int,
    val publicKey: PublicKey,
)
