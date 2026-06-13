package ltd.evilcorp.domain.features.transfer

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.transfer.model.FT_NOT_STARTED
import ltd.evilcorp.domain.features.transfer.model.FT_REJECTED
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.core.model.PublicKey

private const val TAG = "FileTransferManager"

private const val FINGERPRINT_LEN = 8
private fun String.fingerprint() = take(FINGERPRINT_LEN)

fun FileTransferManager.reset() = synchronized(fileTransfers) {
    fileTransfers.clear()
    outgoingFiles.clear()
    scope.launch(ioDispatcher) {
        fileTransferRepository.resetTransientData()
    }
}

fun FileTransferManager.resetForContact(pk: String) = synchronized(fileTransfers) {
    println("$TAG: Clearing fts for contact ${pk.fingerprint()}")
    fileTransfers.filter { it.publicKey == pk }.forEach { ft ->
        setProgress(ft, FT_REJECTED)
        fileTransfers.remove(ft)
        if (ft.outgoing) {
            val uriStr = ft.destination
            outgoingFiles.remove(Pair(pk, ft.fileNumber))?.inputStream?.close()
            platformHelper.releaseFilePermission(uriStr)
        } else {
            scope.launch(ioDispatcher) {
                fileStorageHelper.deleteFile(ft.destination)
            }
        }
    }
}

@Suppress("RedundantSuspendModifier")
suspend fun FileTransferManager.sendAvatar(pkStr: String) = withContext(ioDispatcher) {
    val avatarInfo = fileStorageHelper.getSelfAvatarInfo() ?: return@withContext
    val avatarUriStr = avatarInfo.first
    val size = avatarInfo.second
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
        avatarUriStr
    )
    fileTransfers.add(ft)
    
    val inputStream = platformHelper.openInputStream(avatarUriStr) ?: return@withContext
    outgoingFiles[Pair(pkStr, fileNo)] = OutgoingFile(inputStream, mutableListOf())
}

suspend fun FileTransferManager.broadcastAvatar() = withContext(ioDispatcher) {
    for ((publicKey, _) in toxProfile.getContacts()) {
        val pkStr = publicKey.string()
        val contact = contactRepository.get(pkStr).firstOrNull()
        if (contact != null && contact.connectionStatus != ltd.evilcorp.domain.features.contacts.model.ConnectionStatus.None) {
            sendAvatar(pkStr)
        }
    }
}

fun FileTransferManager.autoSaveFileToPublicDownloads(ft: FileTransfer) {
    if (ft.outgoing || !userSettingsRepository.settings.value.autoSaveToDownloads) return
    try {
        val path = fileStorageHelper.getPathFromUri(ft.destination) ?: return
        if (!fileStorageHelper.fileExists(ft.destination)) return

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

fun FileTransferManager.getCacheSize(): Long {
    return fileStorageHelper.getCacheSize()
}

fun FileTransferManager.clearCache() {
    fileStorageHelper.clearCache()
}

suspend fun FileTransferManager.deleteAll(publicKey: PublicKey) = withContext(ioDispatcher) {
    fileTransferRepository.get(publicKey.string()).take(1).collect { fts ->
        fts.forEach { delete(it.id) }
    }
}
