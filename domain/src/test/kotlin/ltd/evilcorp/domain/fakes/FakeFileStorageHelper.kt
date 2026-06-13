package ltd.evilcorp.domain.fakes

import ltd.evilcorp.domain.features.transfer.IFileStorageHelper

class FakeFileStorageHelper : IFileStorageHelper {
    var emptyFileCreated = false
    var emptyFileResult = true
    var chunksWritten = mutableListOf<Triple<String, Long, ByteArray>>()
    var writeChunkResult = true
    var deletedFiles = mutableListOf<String>()
    var deleteFileResult = true
    var localDestPathResult = "/fake/local/destination"
    var wipAvatarPathResult = "/fake/wip/avatar"
    var finalizeAvatarResult: String? = "/fake/final/avatar"
    var selfAvatarInfoResult: Pair<String, Long>? = Pair("avatar.png", 512L)
    var cacheSizeVal = 0L
    var clearCacheResult = true
    var fileExistsResult = true

    override fun createEmptyFile(destinationUri: String, size: Long): Boolean {
        emptyFileCreated = true
        return emptyFileResult
    }

    override fun writeChunk(destinationUri: String, position: Long, data: ByteArray): Boolean {
        chunksWritten.add(Triple(destinationUri, position, data))
        return writeChunkResult
    }

    override fun deleteFile(destinationUri: String): Boolean {
        deletedFiles.add(destinationUri)
        return deleteFileResult
    }

    override fun makeLocalDestinationPath(publicKeyFingerprint: String, fileName: String): String {
        return localDestPathResult
    }

    override fun makeWipAvatarPath(fileName: String): String {
        return wipAvatarPathResult
    }

    override fun finalizeAvatar(tempName: String, finalName: String): String? {
        return finalizeAvatarResult
    }

    override fun getSelfAvatarInfo(): Pair<String, Long>? {
        return selfAvatarInfoResult
    }

    override fun getCacheSize(): Long = cacheSizeVal

    override fun clearCache(): Boolean = clearCacheResult

    override fun fileExists(destinationUri: String): Boolean = fileExistsResult

    override fun getPathFromUri(destinationUri: String): String? {
        return destinationUri.removePrefix("file://")
    }
}
