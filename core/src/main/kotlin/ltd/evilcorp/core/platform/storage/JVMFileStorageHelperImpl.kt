package ltd.evilcorp.core.platform.storage

import ltd.evilcorp.domain.features.transfer.IFileStorageHelper
import ltd.evilcorp.domain.features.transfer.IFileTransferPlatformHelper
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JVMFileStorageHelperImpl @Inject constructor(
    private val platformHelper: IFileTransferPlatformHelper
) : IFileStorageHelper {
    
    init {
        File(platformHelper.getFilesDir(), "ft").mkdir()
        File(platformHelper.getFilesDir(), "avatar").mkdir()
    }

    override fun createEmptyFile(destinationUri: String, size: Long): Boolean {
        return try {
            val path = getPathFromUri(destinationUri) ?: return false
            val file = File(path)
            file.parentFile?.mkdirs()
            RandomAccessFile(file, "rwd").use { it.setLength(size) }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun writeChunk(destinationUri: String, position: Long, data: ByteArray): Boolean {
        return try {
            val path = getPathFromUri(destinationUri) ?: return false
            RandomAccessFile(File(path), "rwd").use {
                it.seek(position)
                it.write(data)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun deleteFile(destinationUri: String): Boolean {
        val path = getPathFromUri(destinationUri) ?: return false
        return File(path).delete()
    }

    override fun makeLocalDestinationPath(publicKeyFingerprint: String, fileName: String): String {
        val ext = File(fileName).extension
        val suffix = if (ext.isNotEmpty()) ".$ext" else ""
        val baseDir = File(File(platformHelper.getFilesDir(), "ft"), publicKeyFingerprint)
        baseDir.mkdirs()
        return File(baseDir, "${kotlin.random.Random.nextLong()}$suffix").absolutePath
    }

    override fun makeWipAvatarPath(fileName: String): String {
        return android.net.Uri.fromFile(File(File(platformHelper.getFilesDir(), "avatar"), "$fileName.wip")).toString()
    }

    override fun finalizeAvatar(tempName: String, finalName: String): String? {
        val wip = File(File(platformHelper.getFilesDir(), "avatar"), "$tempName.wip")
        val dest = File(File(platformHelper.getFilesDir(), "avatar"), finalName)
        if (wip.exists()) {
            wip.copyTo(dest, overwrite = true)
            wip.delete()
            return android.net.Uri.fromFile(dest).toString()
        }
        return null
    }

    override fun getSelfAvatarInfo(): Pair<String, Long>? {
        val file = File(platformHelper.getFilesDir(), "self_avatar.png")
        if (file.exists() && file.length() > 0L) {
            return Pair(android.net.Uri.fromFile(file).toString(), file.length())
        }
        return null
    }

    override fun getCacheSize(): Long {
        var size = 0L
        val outgoingDir = File(platformHelper.getCacheDir(), "outgoing")
        if (outgoingDir.exists()) size += getFolderSize(outgoingDir)
        val ftDir = File(platformHelper.getFilesDir(), "ft")
        if (ftDir.exists()) size += getFolderSize(ftDir)
        return size
    }

    override fun clearCache(): Boolean {
        return try {
            val outgoingDir = File(platformHelper.getCacheDir(), "outgoing")
            if (outgoingDir.exists()) {
                outgoingDir.listFiles()?.forEach { it.delete() }
            }
            val ftDir = File(platformHelper.getFilesDir(), "ft")
            if (ftDir.exists()) {
                ftDir.deleteRecursively()
                ftDir.mkdir()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getFolderSize(file: File): Long {
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { size += getFolderSize(it) }
        return size
    }

    override fun fileExists(destinationUri: String): Boolean {
        val path = getPathFromUri(destinationUri) ?: return false
        return File(path).exists()
    }

    override fun getPathFromUri(destinationUri: String): String? {
        if (destinationUri.startsWith("file://")) {
            try {
                return java.net.URI(destinationUri).path
            } catch (e: Exception) {
                // fallback manual parse
                return destinationUri.substringAfter("file://")
            }
        }
        return null
    }
}
