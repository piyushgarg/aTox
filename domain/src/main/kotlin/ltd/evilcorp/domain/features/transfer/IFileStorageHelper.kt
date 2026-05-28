package ltd.evilcorp.domain.features.transfer

@Suppress("ComplexInterface")
interface IFileStorageHelper {
    fun createEmptyFile(destinationUri: String, size: Long): Boolean
    fun writeChunk(destinationUri: String, position: Long, data: ByteArray): Boolean
    fun deleteFile(destinationUri: String): Boolean
    fun makeLocalDestinationPath(publicKeyFingerprint: String, fileName: String): String
    fun makeWipAvatarPath(fileName: String): String
    fun finalizeAvatar(tempName: String, finalName: String): String?
    fun getSelfAvatarInfo(): Pair<String, Long>?
    fun getCacheSize(): Long
    fun clearCache(): Boolean
    fun fileExists(destinationUri: String): Boolean
    fun getPathFromUri(destinationUri: String): String?
}
