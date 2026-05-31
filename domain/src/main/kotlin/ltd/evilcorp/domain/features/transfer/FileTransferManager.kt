// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.transfer

import ltd.evilcorp.domain.core.network.enums.ToxFileControl
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.transfer.model.FT_NOT_STARTED
import ltd.evilcorp.domain.features.transfer.model.FT_REJECTED
import ltd.evilcorp.domain.features.transfer.model.FT_STARTED
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.transfer.model.isComplete
import ltd.evilcorp.domain.features.transfer.model.isRejected
import ltd.evilcorp.domain.core.network.MAX_AVATAR_SIZE
import ltd.evilcorp.domain.core.network.IToxFileTransmitter
import ltd.evilcorp.domain.core.network.IToxProfile

private const val TAG = "FileTransferManager"

private const val FINGERPRINT_LEN = 8
private fun String.fingerprint() = take(FINGERPRINT_LEN)

@Singleton
class FileTransferManager @Inject constructor(
    internal val scope: CoroutineScope,
    private val storageCoordinator: FileStoragePlatformCoordinator,
    private val repositories: FileTransferRepositories,
    internal val tox: IToxFileTransmitter,
    internal val toxProfile: IToxProfile,
    private val sessionRegistry: IFileTransferSessionRegistry,
) {
    internal val platformHelper get() = storageCoordinator.platformHelper
    internal val fileStorageHelper get() = storageCoordinator.fileStorageHelper
    internal val contactRepository get() = repositories.contact
    internal val messageRepository get() = repositories.message
    internal val fileTransferRepository get() = repositories.transfer
    internal val userSettingsRepository get() = repositories.userSettings

    internal val fileTransfers get() = sessionRegistry.fileTransfers
    internal val outgoingFiles get() = sessionRegistry.outgoingFiles

    suspend fun add(ft: FileTransfer): Int {
        println("$TAG: Add ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
        
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
                    Message(ft.publicKey, ft.fileName, Sender.Received, MessageType.FileTransfer, id, System.currentTimeMillis()),
                )
                fileTransfers.add(ft.copy().apply { this.id = id })
                id
            }
            FileKind.Avatar.ordinal -> {
                if (ft.fileSize == 0L) {
                    scope.launch(Dispatchers.IO) {
                        contactRepository.setAvatarUri(ft.publicKey, "")
                    }
                    reject(ft)
                    return -1
                } else if (ft.fileSize > MAX_AVATAR_SIZE) {
                    println("$TAG: Got trash avatar with size ${ft.fileSize} from ${ft.publicKey}")
                    scope.launch(Dispatchers.IO) {
                        contactRepository.setAvatarUri(ft.publicKey, "")
                    }
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
        fileTransfers.find { it.id == id }?.let { accept(it) } ?: println("$TAG: Unable to find & accept ft $id")
    }

    fun accept(ft: FileTransfer) {
        println("$TAG: Accept ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
        scope.launch(Dispatchers.IO) {
            val destUri = when (ft.fileKind) {
                FileKind.Data.ordinal -> {
                    val destPath = fileStorageHelper.makeLocalDestinationPath(ft.publicKey.fingerprint(), ft.fileName)
                    "file://$destPath"
                }
                FileKind.Avatar.ordinal -> {
                    fileStorageHelper.makeWipAvatarPath(ft.fileName)
                }
                else -> {
                    println("$TAG: Got unknown file kind when accepting ft: $ft")
                    return@launch
                }
            }

            try {
                if (fileStorageHelper.createEmptyFile(destUri, ft.fileSize)) {
                    setDestination(ft, destUri)
                    setProgress(ft, FT_STARTED)
                    tox.startFileTransfer(PublicKey(ft.publicKey), ft.fileNumber)
                } else {
                    println("$TAG: Failed to create empty file for destination $destUri")
                }
            } catch (e: Exception) {
                println("$TAG: Failed to accept file transfer: ${e.message}")
            }
        }
    }

    fun reject(id: Int) {
        fileTransfers.find { it.id == id }?.let { reject(it) } ?: println("$TAG: Unable to find & reject ft $id")
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
                    fileStorageHelper.deleteFile(uriStr)
                }
            }
        }
    }

    internal fun setDestination(ft: FileTransfer, destination: String) {
        ft.destination = destination
        if (ft.fileKind == FileKind.Data.ordinal) {
            scope.launch(Dispatchers.IO) {
                fileTransferRepository.setDestination(ft.id, destination)
            }
        }
    }

    internal fun setProgress(ft: FileTransfer, progress: Long) {
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

        fileStorageHelper.writeChunk(ft.destination, position, data)
        setProgress(ft, ft.progress + data.size)

        if (ft.isComplete()) {
            println("$TAG: Finished ${ft.fileNumber} for ${ft.publicKey.fingerprint()}")
            if (ft.fileKind == FileKind.Avatar.ordinal) {
                val finalAvatarUri = fileStorageHelper.finalizeAvatar(ft.fileName, ft.fileName)
                if (finalAvatarUri != null) {
                    val avatarUriWithTimestamp = "$finalAvatarUri?t=${System.currentTimeMillis()}"
                    scope.launch(Dispatchers.IO) {
                        contactRepository.setAvatarUri(ft.publicKey, avatarUriWithTimestamp)
                    }
                }
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

        val cachedFileUriStr = try {
            platformHelper.copyToOutgoingCache(fileUriString, name)
        } catch (e: Exception) {
            println("$TAG: Failed to copy outgoing file to cache: ${e.message}")
            return@withContext
        }

        val fileNo = tox.sendFile(pk, FileKind.Data, size, name)
        
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
            Message(ft.publicKey, ft.fileName, Sender.Sent, MessageType.FileTransfer, id, System.currentTimeMillis()),
        )
        fileTransfers.add(ft.copy().apply { this.id = id })

        val inputStream = platformHelper.openInputStream(cachedFileUriStr)
        if (inputStream == null) {
            reject(ft)
            return@withContext
        }
        outgoingFiles[Pair(ft.publicKey, ft.fileNumber)] = OutgoingFile(inputStream, mutableListOf())
    }

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

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        fileTransfers.find { it.id == id }?.let {
            if (!it.isComplete() && !it.isRejected()) {
                reject(it)
            }
            fileTransfers.remove(it)
        }
        fileTransferRepository.get(id).take(1).collect {
            if (!it.outgoing) {
                fileStorageHelper.deleteFile(it.destination)
            }
            fileTransferRepository.delete(id)
        }
    }

    fun get(id: Int) = fileTransferRepository.get(id)
}
