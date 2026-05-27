// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.feature

import ltd.evilcorp.domain.repository.IUserSettingsRepository
import ltd.evilcorp.domain.tox.enums.ToxFileControl
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
import ltd.evilcorp.domain.repository.IContactRepository
import ltd.evilcorp.domain.repository.IFileTransferRepository
import ltd.evilcorp.domain.repository.IMessageRepository
import ltd.evilcorp.domain.model.FT_NOT_STARTED
import ltd.evilcorp.domain.model.FT_REJECTED
import ltd.evilcorp.domain.model.FT_STARTED
import ltd.evilcorp.domain.model.FileKind
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.model.isComplete
import ltd.evilcorp.domain.model.isStarted
import ltd.evilcorp.domain.model.isRejected
import ltd.evilcorp.domain.tox.MAX_AVATAR_SIZE
import ltd.evilcorp.domain.tox.ITox

private const val TAG = "FileTransferManager"

private const val FINGERPRINT_LEN = 8
private fun String.fingerprint() = take(FINGERPRINT_LEN)

@Suppress("ArrayInDataClass")
private data class Chunk(val pos: Long, val data: ByteArray)

private data class OutgoingFile(val inputStream: InputStream, val unsentChunks: MutableList<Chunk>)

@Singleton
@Suppress("LargeClass")
class FileTransferManager @Inject constructor(
    private val scope: CoroutineScope,
    private val platformHelper: IFileTransferPlatformHelper,
    private val contactRepository: IContactRepository,
    private val messageRepository: IMessageRepository,
    private val fileTransferRepository: IFileTransferRepository,
    private val tox: ITox,
    private val userSettingsRepository: IUserSettingsRepository,
) {
    private val fileTransfers: MutableList<FileTransfer> = mutableListOf()
    private val outgoingFiles = mutableMapOf<Pair<String, Int>, OutgoingFile>()

    init {
        File(platformHelper.getFilesDir(), "ft").mkdir()
        File(platformHelper.getFilesDir(), "avatar").mkdir()
    }

    fun reset() {
        fileTransfers.clear()
        scope.launch(Dispatchers.IO) {
            fileTransferRepository.resetTransientData()
        }
    }

    fun resetForContact(pk: String) {
        println("$TAG: Clearing fts for contact ${pk.fingerprint()}")
        fileTransfers.filter { it.publicKey == pk }.kForEach { ft ->
            setProgress(ft, FT_REJECTED)
            fileTransfers.remove(ft)
            if (ft.outgoing) {
                val uriStr = ft.destination
                outgoingFiles.remove(Pair(pk, ft.fileNumber))?.inputStream?.close()
                platformHelper.releaseFilePermission(uriStr)
            } else {
                scope.launch(Dispatchers.IO) {
                    val path = getPathFromUri(ft.destination)
                    if (path != null) {
                        File(path).delete()
                    }
                }
            }
        }
    }

    fun add(ft: FileTransfer): Int {
        println("$TAG: Add ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
        
        // Defensive clean-up of stale/dead transfers with the same reused fileNumber
        val existing = fileTransfers.find { it.publicKey == ft.publicKey && it.fileNumber == ft.fileNumber }
        if (existing != null) {
            println("$TAG: Found stale active transfer with fileNumber ${ft.fileNumber} for ${ft.publicKey.fingerprint()}, removing it")
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
                    println("$TAG: Got trash avatar with size ${ft.fileSize} from ${ft.publicKey}")
                    contactRepository.setAvatarUri(ft.publicKey, "")
                    tox.stopFileTransfer(PublicKey(ft.publicKey), ft.fileNumber)
                    return -1
                }

                fileTransfers.add(ft)
                accept(ft)
                -1
            }
            else -> {
                println("$TAG: Got unknown file kind ${ft.fileKind} in file transfer")
                -1
            }
        }
    }

    fun accept(id: Int) {
        fileTransfers.find { it.id == id }?.let {
            accept(it)
        } ?: println("$TAG: Unable to find & accept ft $id")
    }

    fun accept(ft: FileTransfer) {
        println("$TAG: Accept ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
        scope.launch(Dispatchers.IO) {
            val file = when (ft.fileKind) {
                FileKind.Data.ordinal -> {
                    val destPath = makeDestination(ft)
                    val file = File(destPath)
                    file.parentFile!!.mkdirs()
                    file
                }
                FileKind.Avatar.ordinal -> {
                    val file = wipAvatar(ft.fileName)
                    file.parentFile?.mkdirs()
                    file
                }
                else -> {
                    println("$TAG: Got unknown file kind when accepting ft: $ft")
                    return@launch
                }
            }

            try {
                RandomAccessFile(file, "rwd").use { it.setLength(ft.fileSize) }
                setDestination(ft, file.toURI().toString())
                setProgress(ft, FT_STARTED)
                tox.startFileTransfer(PublicKey(ft.publicKey), ft.fileNumber)
            } catch (e: Exception) {
                println("$TAG: Failed to accept file transfer: ${e.message}")
            }
        }
    }

    fun reject(id: Int) {
        fileTransfers.find { it.id == id }?.let {
            reject(it)
        } ?: println("$TAG: Unable to find & reject ft $id")
    }

    fun reject(ft: FileTransfer) {
        println("$TAG: Reject ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
        fileTransfers.remove(ft)
        tox.stopFileTransfer(PublicKey(ft.publicKey), ft.fileNumber)
        scope.launch(Dispatchers.IO) {
            setProgress(ft, FT_REJECTED)
            if (ft.destination.isNotEmpty()) {
                val uriStr = ft.destination
                if (ft.outgoing) {
                    outgoingFiles.remove(Pair(ft.publicKey, ft.fileNumber))?.inputStream?.close()
                    platformHelper.releaseFilePermission(uriStr)
                } else {
                    val path = getPathFromUri(uriStr)
                    if (path != null) {
                        File(path).delete()
                    }
                }
            }
        }
    }

    private fun setDestination(ft: FileTransfer, destination: String) {
        ft.destination = destination
        if (ft.fileKind == FileKind.Data.ordinal) {
            scope.launch(Dispatchers.IO) {
                fileTransferRepository.setDestination(ft.id, destination)
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
                println("$TAG: Got data for ft $fileNumber for ${publicKey.fingerprint()} we don't know about")
            }
            return
        }

        if (ft.fileKind != FileKind.Data.ordinal && ft.fileKind != FileKind.Avatar.ordinal) {
            println("$TAG: Got unknown file kind when adding data to ft: $ft")
            return
        }

        val path = getPathFromUri(ft.destination)
        if (path != null) {
            RandomAccessFile(File(path), "rwd").use {
                it.seek(position)
                it.write(data)
            }
        }

        setProgress(ft, ft.progress + data.size)

        if (ft.isComplete()) {
            println("$TAG: Finished ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
            if (ft.fileKind == FileKind.Avatar.ordinal) {
                wipAvatar(ft.fileName).copyTo(avatar(ft.fileName), overwrite = true)
                wipAvatar(ft.fileName).delete()
                val avatarUriWithTimestamp = "${avatar(ft.fileName).toURI()}?t=${System.currentTimeMillis()}"
                contactRepository.setAvatarUri(ft.publicKey, avatarUriWithTimestamp)
            } else if (ft.fileKind == FileKind.Data.ordinal) {
                autoSaveFileToPublicDownloads(ft)
            }
            fileTransfers.remove(ft)
        }
    }

    fun transfersFor(publicKey: PublicKey) = fileTransferRepository.get(publicKey.string())

    suspend fun create(pk: PublicKey, fileUriString: String) = withContext(Dispatchers.IO) {
        val info = platformHelper.getFileSizeAndName(fileUriString) ?: return@withContext
        val name = info.first
        val size = info.second

        // Copy the chosen file to the app's cache directory to get a permanent file:// URI
        val cachedFileUriStr = try {
            platformHelper.copyToOutgoingCache(fileUriString, name)
        } catch (e: Exception) {
            println("$TAG: Failed to copy outgoing file to cache: ${e.message}")
            return@withContext
        }

        val fileNo = tox.sendFile(pk, FileKind.Data, size, name)
        
        // Defensive clean-up of stale/dead transfers with the same reused fileNumber
        val existing = fileTransfers.find { it.publicKey == pk.string() && it.fileNumber == fileNo }
        if (existing != null) {
            println("$TAG: Found stale active outgoing transfer with fileNumber $fileNo, removing it")
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
            cachedFileUriStr,
        )
        val id = fileTransferRepository.add(ft).toInt()
        messageRepository.add(
            Message(ft.publicKey, ft.fileName, Sender.Sent, MessageType.FileTransfer, id, Date().time),
        )
        fileTransfers.add(ft.copy().apply { this.id = id })

        val inputStream = platformHelper.openInputStream(cachedFileUriStr)
        if (inputStream == null) {
            reject(ft)
            return@withContext
        }
        outgoingFiles[Pair(ft.publicKey, ft.fileNumber)] = OutgoingFile(inputStream, mutableListOf())
    }

    // TODO(robinlinden): Handle seek-backs: https://github.com/TokTok/c-toxcore/blob/eeaa039222e7a123c2585c8486ee965017767209/toxcore/tox.h#L2405-L2406
    // TODO(robinlinden): An error when sending the last chunk in a transfer will stall it.
    fun sendChunk(pk: String, fileNo: Int, pos: Long, length: Int) {
        val ft = fileTransfers.find { it.publicKey == pk && it.fileNumber == fileNo }
        if (ft == null) {
            println("$TAG: Received request for chunk of unknown ft ${pk.fingerprint()} $fileNo")
            tox.stopFileTransfer(PublicKey(pk), fileNo)
            return
        }

        if (length == 0) {
            println("$TAG: Finished outgoing ft ${pk.fingerprint()} $fileNo ${ft.isComplete()}")
            fileTransfers.remove(ft)
            outgoingFiles.remove(Pair(pk, fileNo))?.inputStream?.close()
            platformHelper.releaseFilePermission(ft.destination)
            return
        }

        val file = outgoingFiles[Pair(pk, fileNo)] ?: return

        while (file.unsentChunks.isNotEmpty()) {
            val chunk = file.unsentChunks.first()
            println("$TAG: Resending chunk @ ${chunk.pos} to ${pk.fingerprint()} ($fileNo)}")
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
        println("$TAG: Setting ${pk.fingerprint()} $fileNo to status $fileStatus")
        val ft = fileTransfers.find { it.publicKey == pk && it.fileNumber == fileNo }
        if (ft == null) {
            println("$TAG: Attempted to set status for unknown ft ${pk.fingerprint()} $fileNo")
            return
        }

        if (fileStatus == ToxFileControl.RESUME && ft.progress == FT_NOT_STARTED) {
            ft.progress = FT_STARTED
        } else if (fileStatus == ToxFileControl.CANCEL) {
            println("$TAG: Friend canceled ft ${pk.fingerprint()} $fileNo")
            reject(ft)
        }
    }

    suspend fun deleteAll(publicKey: PublicKey) = withContext(Dispatchers.IO) {
        fileTransferRepository.get(publicKey.string()).take(1).collect { fts ->
            fts.kForEach { delete(it.id) }
        }
    }

    suspend fun sendAvatar(pkStr: String) = withContext(Dispatchers.IO) {
        val selfAvatarFile = File(platformHelper.getFilesDir(), "self_avatar.png")
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
            println("$TAG: Failed to initiate avatar send to ${pkStr.fingerprint()}: ${e.message}")
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
            selfAvatarFile.toURI().toString()
        )
        fileTransfers.add(ft)
        
        val inputStream = selfAvatarFile.inputStream()
        outgoingFiles[Pair(pkStr, fileNo)] = OutgoingFile(inputStream, mutableListOf())
    }

    suspend fun broadcastAvatar() = withContext(Dispatchers.IO) {
        for ((publicKey, _) in tox.getContacts()) {
            val pkStr = publicKey.string()
            val contact = contactRepository.get(pkStr).firstOrNull()
            if (contact != null && contact.connectionStatus != ltd.evilcorp.domain.model.ConnectionStatus.None) {
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
                val path = getPathFromUri(it.destination)
                if (path != null) {
                    File(path).delete()
                }
            }
            fileTransferRepository.delete(id)
        }
    }

    fun get(id: Int) = fileTransferRepository.get(id)

    private fun makeDestination(ft: FileTransfer): String {
        val ext = File(ft.fileName).extension
        val suffix = if (ext.isNotEmpty()) ".$ext" else ""
        return File(File(File(platformHelper.getFilesDir(), "ft"), ft.publicKey.fingerprint()), "${Random.nextLong()}$suffix").absolutePath
    }

    private fun wipAvatar(name: String): File = File(File(platformHelper.getFilesDir(), "avatar"), "$name.wip")
    private fun avatar(name: String): File = File(File(platformHelper.getFilesDir(), "avatar"), name)

    private fun autoSaveFileToPublicDownloads(ft: FileTransfer) {
        if (ft.outgoing || !userSettingsRepository.settings.value.autoSaveToDownloads) return
        try {
            val path = getPathFromUri(ft.destination) ?: return
            val sourceFile = File(path)
            if (!sourceFile.exists()) return

            val configuredDirectory = userSettingsRepository.settings.value.autoSaveDirectoryUri
            if (configuredDirectory.isNotBlank()) {
                val targetUriStr = platformHelper.autoSaveFileToDirectory(ft.fileName, path, configuredDirectory)
                if (targetUriStr != null) {
                    println("$TAG: Successfully auto-saved ${ft.fileName} to configured directory at $targetUriStr")
                    setDestination(ft, targetUriStr)
                }
                return
            }

            val publicUriStr = platformHelper.autoSaveFileToPublicDownloads(ft.fileName, path)
            if (publicUriStr != null) {
                println("$TAG: Successfully auto-saved ${ft.fileName} to public Downloads/aTox at $publicUriStr")
                setDestination(ft, publicUriStr)
            }
        } catch (e: Exception) {
            println("$TAG: Error auto-saving file ${ft.fileName}: ${e.message}")
        }
    }

    fun getCacheSize(): Long {
        var size = 0L
        val outgoingDir = File(platformHelper.getCacheDir(), "outgoing")
        if (outgoingDir.exists()) {
            size += getFolderSize(outgoingDir)
        }
        val ftDir = File(platformHelper.getFilesDir(), "ft")
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
        val outgoingDir = File(platformHelper.getCacheDir(), "outgoing")
        if (outgoingDir.exists()) {
            val list = outgoingDir.listFiles()
            if (list != null) {
                for (f in list) {
                    f.delete()
                }
            }
        }
        val ftDir = File(platformHelper.getFilesDir(), "ft")
        if (ftDir.exists()) {
            ftDir.deleteRecursively()
            ftDir.mkdir()
        }
    }

    private fun getPathFromUri(uriString: String): String? {
        if (uriString.startsWith("file://")) {
            try {
                return java.net.URI(uriString).path
            } catch (e: Exception) {
                // fallback manual parse
                return uriString.substringAfter("file://")
            }
        }
        return null
    }
}
