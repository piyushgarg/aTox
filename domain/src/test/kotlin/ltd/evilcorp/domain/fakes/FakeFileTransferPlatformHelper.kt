package ltd.evilcorp.domain.fakes

import ltd.evilcorp.domain.core.io.IInputStream
import ltd.evilcorp.domain.features.transfer.IFileTransferPlatformHelper

class FakeFileTransferPlatformHelper : IFileTransferPlatformHelper {
    var filesDirPath = "/fake/files"
    var cacheDirPath = "/fake/cache"
    var fileSizeAndNameResult: Pair<String, Long>? = Pair("test_file.txt", 1024L)
    var cachePathResult = "/fake/cache/test_file.txt"
    var streamToReturn: IInputStream? = null
    var releasedUri: String? = null
    var autoSavePublicResult: String? = "/fake/downloads/test_file.txt"
    var autoSaveDirResult: String? = "/fake/custom_dir/test_file.txt"
    var saveFileResult = true

    override fun getFilesDir(): String = filesDirPath
    override fun getCacheDir(): String = cacheDirPath

    override fun getFileSizeAndName(uriString: String): Pair<String, Long>? {
        return fileSizeAndNameResult
    }

    override fun copyToOutgoingCache(uriString: String, name: String): String {
        return cachePathResult
    }

    override fun openInputStream(uriString: String): IInputStream? {
        return streamToReturn
    }

    override fun releaseFilePermission(uriString: String) {
        releasedUri = uriString
    }

    override fun autoSaveFileToPublicDownloads(fileName: String, sourceFilePath: String): String? {
        return autoSavePublicResult
    }

    override fun autoSaveFileToDirectory(fileName: String, sourceFilePath: String, directoryUriString: String): String? {
        return autoSaveDirResult
    }

    override fun saveFileToUri(sourceFilePath: String, targetUriString: String): Boolean {
        return saveFileResult
    }
}
