package ltd.evilcorp.core.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import ltd.evilcorp.domain.feature.IFileTransferPlatformHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferPlatformHelperImpl @Inject constructor(
    private val context: Context
) : IFileTransferPlatformHelper {

    private val resolver: ContentResolver
        get() = context.contentResolver

    override fun getFilesDir(): String {
        return context.filesDir.absolutePath
    }

    override fun getCacheDir(): String {
        return context.cacheDir.absolutePath
    }

    override fun getFileSizeAndName(uriString: String): Pair<String, Long>? {
        val uri = Uri.parse(uriString)
        return if (uri.scheme == "file") {
            val f = File(uri.path ?: return null)
            Pair(f.name, f.length())
        } else {
            resolver.query(uri, null, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    Pair(name, fileSize)
                } else null
            }
        }
    }

    override fun copyToOutgoingCache(uriString: String, name: String): String {
        val uri = Uri.parse(uriString)
        val cacheDir = File(context.cacheDir, "outgoing")
        cacheDir.mkdirs()
        val destFile = File(cacheDir, "${UUID.randomUUID()}_$name")
        val input = if (uri.scheme == "file") {
            FileInputStream(File(uri.path ?: throw FileNotFoundException("Null path for file URI")))
        } else {
            resolver.openInputStream(uri)
        }
        input?.use { inp ->
            destFile.outputStream().use { output ->
                inp.copyTo(output)
            }
        }
        return Uri.fromFile(destFile).toString()
    }

    override fun openInputStream(uriString: String): InputStream? {
        val uri = Uri.parse(uriString)
        return if (uri.scheme == "file") {
            FileInputStream(File(uri.path ?: return null))
        } else {
            resolver.openInputStream(uri)
        }
    }

    override fun releaseFilePermission(uriString: String) {
        val uri = Uri.parse(uriString)
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            return
        }
        try {
            resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // Ignore
        }
    }

    override fun autoSaveFileToPublicDownloads(fileName: String, sourceFilePath: String): String? {
        try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) return null

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    val ext = sourceFile.extension.lowercase()
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/aTox")
                }

                val publicUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (publicUri != null) {
                    resolver.openOutputStream(publicUri).use { out ->
                        FileInputStream(sourceFile).use { ins ->
                            ins.copyTo(out ?: return@use)
                        }
                    }
                    return publicUri.toString()
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val atoxDir = File(downloadDir, "aTox")
                atoxDir.mkdirs()
                val targetFile = File(atoxDir, fileName)
                sourceFile.copyTo(targetFile, overwrite = true)
                return Uri.fromFile(targetFile).toString()
            }
        } catch (e: Exception) {
            android.util.Log.e("FileTransferHelper", "Failed to auto-save file to public downloads", e)
        }
        return null
    }

    override fun autoSaveFileToDirectory(fileName: String, sourceFilePath: String, directoryUriString: String): String? {
        try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) return null
            val directoryUri = Uri.parse(directoryUriString)
            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(sourceFile.extension.lowercase())
                ?: "application/octet-stream"
            val targetUri = DocumentsContract.createDocument(resolver, directoryUri, mimeType, fileName)
                ?: error("Failed to create $fileName in $directoryUri")
            resolver.openOutputStream(targetUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: error("Failed to open $targetUri for writing")
            return targetUri.toString()
        } catch (e: Exception) {
            android.util.Log.e("FileTransferHelper", "Failed to auto-save file to directory", e)
        }
        return null
    }
}
