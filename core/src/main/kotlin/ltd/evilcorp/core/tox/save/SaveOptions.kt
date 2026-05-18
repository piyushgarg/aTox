package ltd.evilcorp.core.tox.save

enum class ProxyType {
    None,
    HTTP,
    SOCKS5,
}

@Suppress("ArrayInDataClass")
data class SaveOptions(
    val saveData: ByteArray?,
    val udpEnabled: Boolean,
    val proxyType: ProxyType,
    val proxyAddress: String,
    val proxyPort: Int,
)
