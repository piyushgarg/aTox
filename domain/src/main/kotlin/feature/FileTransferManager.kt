// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.feature

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import ltd.evilcorp.core.repository.UserSettingsRepository
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import ltd.evilcorp.core.tox.enums.ToxFileControl
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.forEach as kForEach
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.FileTransferRepository
import ltd.evilcorp.core.repository.MessageRepository
import ltd.evilcorp.core.model.FT_NOT_STARTED
import ltd.evilcorp.core.model.FT_REJECTED
import ltd.evilcorp.core.model.FT_STARTED
import ltd.evilcorp.core.model.FileKind
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.model.isComplete
import ltd.evilcorp.core.model.isStarted
import ltd.evilcorp.core.model.isRejected
import ltd.evilcorp.core.tox.MAX_AVATAR_SIZE
import ltd.evilcorp.domain.tox.Tox

private const val TAG = "FileTransferManager"

// TODO(robinlinden): This will go away when PublicKey is used everywhere it should be.
private const val FINGERPRINT_LEN = 8
private fun String.fingerprint() = take(FINGERPRINT_LEN)

@Suppress("ArrayInDataClass")
private data class Chunk(val pos: Long, val data: ByteArray)

private data class OutgoingFile(val inputStream: InputStream, val unsentChunks: MutableList<Chunk>)

@Singleton
class FileTransferManager @Inject constructor(
    private val scope: CoroutineScope,
    private val context: Context,
    private val resolver: ContentResolver,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val fileTransferRepository: FileTransferRepository,
    private val tox: Tox,
    private val userSettingsRepository: UserSettingsRepository,
) {
    private val fileTransfers: MutableList<FileTransfer> = mutableListOf()
    private val outgoingFiles = mutableMapOf<Pair<String, Int>, OutgoingFile>()

    init {
        File(context.filesDir, "ft").mkdir()
        File(context.filesDir, "avatar").mkdir()
    }

    fun reset() {
        fileTransfers.clear()
        scope.launch(Dispatchers.IO) {
            fileTransferRepository.resetTransientData()
        }
    }

    fun resetForContact(pk: String) {
        Log.i(TAG, "Clearing fts for contact ${pk.fingerprint()}")
        fileTransfers.filter { it.publicKey == pk }.kForEach { ft ->
            setProgress(ft, FT_REJECTED)
            fileTransfers.remove(ft)
            if (ft.outgoing) {
                val uri = ft.destination.toUri()
                outgoingFiles.remove(Pair(pk, ft.fileNumber))?.inputStream?.close()
                releaseFilePermission(uri)
            } else {
                scope.launch(Dispatchers.IO) {
                    val path = ft.destination.toUri().path
                    if (path != null) {
                        File(path).delete()
                    }
                }
            }
        }
    }

    fun add(ft: FileTransfer): Int {
        Log.i(TAG, "Add ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
        
        // Defensive clean-up of stale/dead transfers with the same reused fileNumber
        val existing = fileTransfers.find { it.publicKey == ft.publicKey && it.fileNumber == ft.fileNumber }
        if (existing != null) {
            Log.w(TAG, "Found stale active transfer with fileNumber ${ft.fileNumber} for ${ft.publicKey.fingerprint()}, removing it")
            fileTransfers.remove(existing)
            scope.launch(Dispatchers.IO) {
                fileTransferRepository.updateProgress(existing.id, FT_REJECTED)
            }
        }

        return when (ft.fileKind) {
            FileKind.Data.ordinal -> {
                val id = fileTransferRepository.add(ft).toInt()
                messageRepository.add(
                    Message(ft.publicKey, ft.fileName, Sender.Received, MessageType.FileTransfer, id, Date().time),
                )
                fileTransfers.add(ft.copy().apply { this.id = id })
                id
            }
            FileKind.Avatar.ordinal -> {
                if (ft.fileSize == 0L) {
                    contactRepository.setAvatarUri(ft.publicKey, "")
                    reject(ft)
                    return -1
                } else if (ft.fileSize > MAX_AVATAR_SIZE) {
                    Log.e(TAG, "Got trash avatar with size ${ft.fileSize} from ${ft.publicKey}")
                    contactRepository.setAvatarUri(ft.publicKey, "")
                    tox.stopFileTransfer(PublicKey(ft.publicKey), ft.fileNumber)
                    return -1
                }

                fileTransfers.add(ft)
                accept(ft)
                -1
            }
            else -> {
                Log.e(TAG, "Got unknown file kind ${ft.fileKind} in file transfer")
                -1
            }
        }
    }

    fun accept(id: Int) {
        fileTransfers.find { it.id == id }?.let {
            accept(it)
        } ?: Log.e(TAG, "Unable to find & accept ft $id")
    }

    fun accept(ft: FileTransfer) {
        Log.i(TAG, "Accept ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
        scope.launch(Dispatchers.IO) {
            val file = when (ft.fileKind) {
                FileKind.Data.ordinal -> {
                    val dest = makeDestination(ft)
                    val file = File(dest.path!!)
                    file.parentFile!!.mkdirs()
                    file
                }
                FileKind.Avatar.ordinal -> wipAvatar(ft.fileName)
                else -> {
                    Log.e(TAG, "Got unknown file kind when accepting ft: $ft")
                    return@launch
                }
            }

            try {
                RandomAccessFile(file, "rwd").use { it.setLength(ft.fileSize) }
                setDestination(ft, Uri.fromFile(file))
                setProgress(ft, FT_STARTED)
                tox.startFileTransfer(PublicKey(ft.publicKey), ft.fileNumber)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to accept file transfer", e)
            }
        }
    }

    fun reject(id: Int) {
        fileTransfers.find { it.id == id }?.let {
            reject(it)
        } ?: Log.e(TAG, "Unable to find & reject ft $id")
    }

    fun reject(ft: FileTransfer) {
        Log.i(TAG, "Reject ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
        fileTransfers.remove(ft)
        tox.stopFileTransfer(PublicKey(ft.publicKey), ft.fileNumber)
        scope.launch(Dispatchers.IO) {
            setProgress(ft, FT_REJECTED)
            if (ft.destination.isNotEmpty()) {
                val uri = ft.destination.toUri()
                if (ft.outgoing) {
                    outgoingFiles.remove(Pair(ft.publicKey, ft.fileNumber))?.inputStream?.close()
                    releaseFilePermission(uri)
                } else {
                    uri.path?.let { File(it).delete() }
                }
            }
        }
    }

    private fun setDestination(ft: FileTransfer, destination: Uri) {
        ft.destination = destination.toString()
        if (ft.fileKind == FileKind.Data.ordinal) {
            scope.launch(Dispatchers.IO) {
                fileTransferRepository.setDestination(ft.id, destination.toString())
            }
        }
    }

    private fun setProgress(ft: FileTransfer, progress: Long) {
        ft.progress = progress
        if (ft.fileKind == FileKind.Data.ordinal) {
            scope.launch(Dispatchers.IO) {
                fileTransferRepository.updateProgress(ft.id, progress)
            }
        }
    }

    fun addDataToTransfer(publicKey: String, fileNumber: Int, position: Long, data: ByteArray) {
        val ft = fileTransfers.find { it.publicKey == publicKey && it.fileNumber == fileNumber }
        if (ft == null) {
            if (data.isNotEmpty()) {
                Log.e(TAG, "Got data for ft $fileNumber for ${publicKey.fingerprint()} we don't know about")
            }
            return
        }

        if (ft.fileKind != FileKind.Data.ordinal && ft.fileKind != FileKind.Avatar.ordinal) {
            Log.e(TAG, "Got unknown file kind when adding data to ft: $ft")
            return
        }

        RandomAccessFile(File(ft.destination.toUri().path!!), "rwd").use {
            it.seek(position)
            it.write(data)
        }

        setProgress(ft, ft.progress + data.size)

        if (ft.isComplete()) {
            Log.i(TAG, "Finished ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
            if (ft.fileKind == FileKind.Avatar.ordinal) {
                wipAvatar(ft.fileName).copyTo(avatar(ft.fileName), overwrite = true)
                wipAvatar(ft.fileName).delete()
                contactRepository.setAvatarUri(ft.publicKey, Uri.fromFile(avatar(ft.fileName)).toString())
            } else if (ft.fileKind == FileKind.Data.ordinal) {
                autoSaveFileToPublicDownloads(ft)
            }
            fileTransfers.remove(ft)
        }
    }

    fun transfersFor(publicKey: PublicKey) = fileTransferRepository.get(publicKey.string())

    suspend fun create(pk: PublicKey, file: Uri) = withContext(Dispatchers.IO) {
        val (name, size) = if (file.scheme == "file") {
            val f = File(file.path ?: return@withContext)
            Pair(f.name, f.length())
        } else {
            context.contentResolver.query(file, null, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    Pair(name, fileSize)
                } else null
            }
        } ?: return@withContext

        // Copy the chosen file to the app's cache directory to get a permanent file:// URI
        val cachedFileUri = try {
            copyToOutgoingCache(file, name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy outgoing file to cache", e)
            return@withContext
        }

        val fileNo = tox.sendFile(pk, FileKind.Data, size, name)
        
        // Defensive clean-up of stale/dead transfers with the same reused fileNumber
        val existing = fileTransfers.find { it.publicKey == pk.string() && it.fileNumber == fileNo }
        if (existing != null) {
            Log.w(TAG, "Found stale active outgoing transfer with fileNumber $fileNo, removing it")
            fileTransfers.remove(existing)
            fileTransferRepository.updateProgress(existing.id, FT_REJECTED)
        }

        val ft = FileTransfer(
            pk.string(),
            fileNo,
            FileKind.Data.ordinal,
            size,
            name,
            true,
            FT_NOT_STARTED,
            cachedFileUri.toString(),
        )
        val id = fileTransferRepository.add(ft).toInt()
        messageRepository.add(
            Message(ft.publicKey, ft.fileName, Sender.Sent, MessageType.FileTransfer, id, Date().time),
        )
        fileTransfers.add(ft.copy().apply { this.id = id })

        val inputStream = if (cachedFileUri.scheme == "file") {
            java.io.FileInputStream(File(cachedFileUri.path ?: return@withContext))
        } else {
            resolver.openInputStream(cachedFileUri)
        }
        if (inputStream == null) {
            reject(ft)
            return@withContext
        }
        outgoingFiles[Pair(ft.publicKey, ft.fileNumber)] = OutgoingFile(inputStream, mutableListOf())
    }

    private fun copyToOutgoingCache(uri: Uri, name: String): Uri {
        val cacheDir = File(context.cacheDir, "outgoing")
        cacheDir.mkdirs()
        val destFile = File(cacheDir, "${java.util.UUID.randomUUID()}_$name")
        val input = if (uri.scheme == "file") {
            java.io.FileInputStream(File(uri.path ?: throw java.io.FileNotFoundException("Null path for file URI")))
        } else {
            resolver.openInputStream(uri)
        }
        input?.use { inp ->
            destFile.outputStream().use { output ->
                inp.copyTo(output)
            }
        }
        return destFile.toUri()
    }

    // TODO(robinlinden): Handle seek-backs: https://github.com/TokTok/c-toxcore/blob/eeaa039222e7a123c2585c8486ee965017767209/toxcore/tox.h#L2405-L2406
    // TODO(robinlinden): An error when sending the last chunk in a transfer will stall it.
    fun sendChunk(pk: String, fileNo: Int, pos: Long, length: Int) {
        val ft = fileTransfers.find { it.publicKey == pk && it.fileNumber == fileNo }
        if (ft == null) {
            Log.e(TAG, "Received request for chunk of unknown ft ${pk.fingerprint()} $fileNo")
            tox.stopFileTransfer(PublicKey(pk), fileNo)
            return
        }

        if (length == 0) {
            Log.i(TAG, "Finished outgoing ft ${pk.fingerprint()} $fileNo ${ft.isComplete()}")
            fileTransfers.remove(ft)
            outgoingFiles.remove(Pair(pk, fileNo))?.inputStream?.close()
            releaseFilePermission(ft.destination.toUri())
            return
        }

        val file = outgoingFiles[Pair(pk, fileNo)] ?: return

        while (file.unsentChunks.isNotEmpty()) {
            val chunk = file.unsentChunks.first()
            Log.i(TAG, "Resending chunk @ ${chunk.pos} to ${pk.fingerprint()} ($fileNo)}")
            if (tox.sendFileChunk(PublicKey(pk), fileNo, chunk.pos, chunk.data).isFailure) {
                return
            }
            setProgress(ft, ft.progress + chunk.data.size)
            file.unsentChunks.removeAt(0)
        }

        val bytes = ByteArray(length)
        file.inputStream.read(bytes, 0, length)
        if (tox.sendFileChunk(PublicKey(pk), fileNo, pos, bytes).isFailure) {
            file.unsentChunks.add(Chunk(pos, bytes))
            return
        }

        setProgress(ft, ft.progress + length)
    }

    fun setStatus(pk: String, fileNo: Int, fileStatus: ToxFileControl) {
        Log.e(TAG, "Setting ${pk.fingerprint()} $fileNo to status $fileStatus")
        val ft = fileTransfers.find { it.publicKey == pk && it.fileNumber == fileNo }
        if (ft == null) {
            Log.e(TAG, "Attempted to set status for unknown ft ${pk.fingerprint()} $fileNo")
            return
        }

        if (fileStatus == ToxFileControl.RESUME && ft.progress == FT_NOT_STARTED) {
            ft.progress = FT_STARTED
        } else if (fileStatus == ToxFileControl.CANCEL) {
            Log.i(TAG, "Friend canceled ft ${pk.fingerprint()} $fileNo")
            reject(ft)
        }
    }

    suspend fun deleteAll(publicKey: PublicKey) = withContext(Dispatchers.IO) {
        fileTransferRepository.get(publicKey.string()).take(1).collect { fts ->
            fts.kForEach { delete(it.id) }
        }
    }

    suspend fun sendAvatar(pkStr: String) = withContext(Dispatchers.IO) {
        val selfAvatarFile = File(context.filesDir, "self_avatar.png")
        if (!selfAvatarFile.exists() || selfAvatarFile.length() == 0L) {
            return@withContext
        }
        val size = selfAvatarFile.length()
        val pk = PublicKey(pkStr)
        
        val existing = fileTransfers.find { it.publicKey == pkStr && it.fileKind == FileKind.Avatar.ordinal }
        if (existing != null) {
            fileTransfers.remove(existing)
            outgoingFiles.remove(Pair(pkStr, existing.fileNumber))?.inputStream?.close()
        }

        val fileNo = try {
            tox.sendFile(pk, FileKind.Avatar, size, "avatar")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate avatar send to ${pkStr.fingerprint()}", e)
            -1
        }
        if (fileNo == -1) return@withContext

        val ft = FileTransfer(
            pkStr,
            fileNo,
            FileKind.Avatar.ordinal,
            size,
            "avatar",
            true,
            FT_NOT_STARTED,
            selfAvatarFile.toUri().toString()
        )
        fileTransfers.add(ft)
        
        val inputStream = selfAvatarFile.inputStream()
        outgoingFiles[Pair(pkStr, fileNo)] = OutgoingFile(inputStream, mutableListOf())
    }

    suspend fun broadcastAvatar() = withContext(Dispatchers.IO) {
        for ((publicKey, _) in tox.getContacts()) {
            val pkStr = publicKey.string()
            val contact = contactRepository.get(pkStr).firstOrNull()
            if (contact != null && contact.connectionStatus != ltd.evilcorp.core.model.ConnectionStatus.None) {
                sendAvatar(pkStr)
            }
        }
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        fileTransfers.find { it.id == id }?.let {
            if (!it.isComplete() && !it.isRejected()) {
                reject(it)
            }
            fileTransfers.remove(it)
        }
        fileTransferRepository.get(id).take(1).collect {
            if (!it.outgoing && it.destination.startsWith("file://")) {
                val path = it.destination.toUri().path
                if (path != null) {
                    File(path).delete()
                }
            }
            fileTransferRepository.delete(id)
        }
    }

    fun get(id: Int) = fileTransferRepository.get(id)

    private fun releaseFilePermission(uri: Uri) {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            return
        }

        if (fileTransfers.firstOrNull { it.destination == uri.toString() } != null) {
            return
        }

        Log.i(TAG, "Releasing read permission for $uri")
        try {
            resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to release permission for $uri: ${e.message}")
        }
    }

    private fun makeDestination(ft: FileTransfer): Uri {
        val ext = File(ft.fileName).extension
        val suffix = if (ext.isNotEmpty()) ".$ext" else ""
        return Uri.fromFile(File(File(File(context.filesDir, "ft"), ft.publicKey.fingerprint()), "${Random.nextLong()}$suffix"))
    }

    private fun wipAvatar(name: String): File = File(File(context.filesDir, "avatar"), "$name.wip")
    private fun avatar(name: String): File = File(File(context.filesDir, "avatar"), name)

    private fun autoSaveFileToPublicDownloads(ft: FileTransfer) {
        if (ft.outgoing || !userSettingsRepository.settings.value.autoSaveToDownloads) return
        try {
            val sourceFile = File(ft.destination.toUri().path ?: return)
            if (!sourceFile.exists()) return

            val configuredDirectory = userSettingsRepository.settings.value.autoSaveDirectoryUri
            if (configuredDirectory.isNotBlank()) {
                autoSaveFileToDirectory(ft, sourceFile, configuredDirectory.toUri())
                return
            }

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, ft.fileName)
                val ext = sourceFile.extension.lowercase()
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/aTox")
            }

            val publicUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (publicUri != null) {
                resolver.openOutputStream(publicUri).use { out ->
                    java.io.FileInputStream(sourceFile).use { ins ->
                        ins.copyTo(out ?: return@use)
                    }
                }
                Log.i(TAG, "Successfully auto-saved ${ft.fileName} to public Downloads/aTox at $publicUri")
                setDestination(ft, publicUri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-saving file ${ft.fileName} to public Downloads", e)
        }
    }

    private fun autoSaveFileToDirectory(ft: FileTransfer, sourceFile: File, directoryUri: Uri) {
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(sourceFile.extension.lowercase())
            ?: "application/octet-stream"
        val targetUri = DocumentsContract.createDocument(resolver, directoryUri, mimeType, ft.fileName)
            ?: throw IllegalStateException("Failed to create ${ft.fileName} in $directoryUri")
        resolver.openOutputStream(targetUri)?.use { out ->
            sourceFile.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: throw IllegalStateException("Failed to open $targetUri for writing")
        Log.i(TAG, "Successfully auto-saved ${ft.fileName} to configured directory at $targetUri")
        setDestination(ft, targetUri)
    }

    fun getCacheSize(): Long {
        var size = 0L
        val outgoingDir = File(context.cacheDir, "outgoing")
        if (outgoingDir.exists()) {
            size += getFolderSize(outgoingDir)
        }
        val ftDir = File(context.filesDir, "ft")
        if (ftDir.exists()) {
            size += getFolderSize(ftDir)
        }
        return size
    }

    private fun getFolderSize(file: File): Long {
        if (file.isFile) return file.length()
        var size = 0L
        val list = file.listFiles()
        if (list != null) {
            for (f in list) {
                size += getFolderSize(f)
            }
        }
        return size
    }

    fun clearCache() {
        val outgoingDir = File(context.cacheDir, "outgoing")
        if (outgoingDir.exists()) {
            val list = outgoingDir.listFiles()
            if (list != null) {
                for (f in list) {
                    f.delete()
                }
            }
        }
        val ftDir = File(context.filesDir, "ft")
        if (ftDir.exists()) {
            ftDir.deleteRecursively()
            ftDir.mkdir()
        }
    }
}
