package ltd.evilcorp.domain.core.platform

interface IPlatformServices {
    fun formatDate(timestamp: Long): String
    fun generateSecureBytes(size: Int): ByteArray
    fun zip(files: Map<String, ByteArray>): ByteArray
    fun unzip(zipBytes: ByteArray): Map<String, ByteArray>
}
