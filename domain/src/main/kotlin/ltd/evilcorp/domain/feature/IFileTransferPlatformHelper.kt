package ltd.evilcorp.domain.feature

import java.io.InputStream

interface IFileTransferPlatformHelper {
    fun getFilesDir(): String
    fun getCacheDir(): String
    fun getFileSizeAndName(uriString: String): Pair<String, Long>?
    fun copyToOutgoingCache(uriString: String, name: String): String
    fun openInputStream(uriString: String): InputStream?
    fun releaseFilePermission(uriString: String)
    fun autoSaveFileToPublicDownloads(fileName: String, sourceFilePath: String): String?
    fun autoSaveFileToDirectory(fileName: String, sourceFilePath: String, directoryUriString: String): String?
}
