package ltd.evilcorp.core.model

private fun hexToBytes(hex: String) = hex.chunked(2).map { it.uppercase().toInt(radix = 16).toByte() }.toByteArray()
private fun bytesToHex(bytes: ByteArray) = bytes.joinToString(separator = "") { "%02X".format(it) }

const val FINGERPRINT_LEN = 8

@JvmInline
value class PublicKey(private val value: String) {
    fun bytes() = hexToBytes(value)
    fun string() = value
    fun fingerprint() = value.take(FINGERPRINT_LEN)

    companion object {
        fun fromBytes(publicKey: ByteArray) = PublicKey(bytesToHex(publicKey))
    }
}
