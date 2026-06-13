package ltd.evilcorp.domain.fakes

import ltd.evilcorp.domain.core.platform.IPlatformServices

class FakePlatformServices : IPlatformServices {
    var dateStringToReturn = "2026-05-31 12:00:00"
    var zippedBytesToReturn = byteArrayOf(4, 5, 6)
    var unzippedResult: Map<String, ByteArray> = emptyMap()

    override fun formatDate(timestamp: Long): String {
        return dateStringToReturn
    }

    override fun generateSecureBytes(size: Int): ByteArray {
        return ByteArray(size) { i -> (i + 1).toByte() }
    }

    override fun zip(files: Map<String, ByteArray>): ByteArray {
        return zippedBytesToReturn
    }

    override fun unzip(zipBytes: ByteArray): Map<String, ByteArray> {
        return unzippedResult
    }
}
