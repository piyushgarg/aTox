package ltd.evilcorp.atox.infrastructure.backup

import android.content.ContentResolver
import android.content.Context
import androidx.core.net.toUri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.features.backup.repository.IFileTransferBackupHelper
import ltd.evilcorp.domain.features.transfer.model.FileTransfer

class FileTransferHistoryBackupDataProvider @Inject constructor(
    private val helper: IFileTransferBackupHelper,
) : IBackupDataProvider {
    override val id: String = "file_transfer_history"
    override val displayNameRes: Int = R.string.backup_module_file_transfer_history
    override val descriptionRes: Int = R.string.backup_module_file_transfer_history_description

    override suspend fun serialize(outputStream: java.io.OutputStream) {
        outputStream.write(serializeFileTransfers(helper.serializeFileTransfers()))
    }

    override suspend fun deserialize(data: ByteArray) {
        helper.deserializeFileTransfers(parseFileTransfers(data))
    }
}

class TransferredFilesBackupDataProvider @Inject constructor(
    private val context: Context,
    private val resolver: ContentResolver,
    private val helper: IFileTransferBackupHelper,
) : IBackupDataProvider {
    override val id: String = "transferred_files"
    override val displayNameRes: Int = R.string.backup_module_transferred_files
    override val descriptionRes: Int = R.string.backup_module_transferred_files_description

    override suspend fun serialize(outputStream: java.io.OutputStream) {
        val manifestItems = mutableListOf<TransferredFileManifestItem>()
        val transfers = helper.serializeFileTransfers()
            .filter { it.destination.isNotBlank() }

        // Wrap in a non-closing stream to prevent closing the main backup stream
        val nonClosing = object : java.io.FilterOutputStream(outputStream) {
            override fun close() {
                flush()
            }
        }

        ZipOutputStream(nonClosing).use { zip ->
            transfers.forEach { transfer ->
                val entryName = "files/${transfer.id}-${transfer.fileName.sanitizeZipName()}"
                val copied = runCatching {
                    resolver.openInputStream(transfer.destination.toUri())?.use { input ->
                        zip.putNextEntry(ZipEntry(entryName))
                        input.copyTo(zip)
                        zip.closeEntry()
                        true
                    } ?: false
                }.getOrDefault(false)

                if (copied) {
                    manifestItems.add(
                        TransferredFileManifestItem(
                            id = transfer.id,
                            fileName = transfer.fileName,
                            entryName = entryName
                        )
                    )
                }
            }

            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            val manifest = TransferredFileManifest(files = manifestItems)
            zip.write(Json.encodeToString(manifest).encodeToByteArray())
            zip.closeEntry()
        }
    }

    override suspend fun deserialize(data: ByteArray) {
        val files = mutableMapOf<String, ByteArray>()
        var manifest = TransferredFileManifest(files = emptyList())

        ZipInputStream(ByteArrayInputStream(data)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val bytes = zip.readBytes()
                if (entry.name == MANIFEST_ENTRY) {
                    manifest = Json.decodeFromString<TransferredFileManifest>(bytes.decodeToString())
                } else {
                    files[entry.name] = bytes
                }
                zip.closeEntry()
            }
        }

        val restoreDir = File(context.filesDir, "ft/restored").apply { mkdirs() }
        for (item in manifest.files) {
            val id = item.id
            val entryName = item.entryName
            val fileName = item.fileName.sanitizeFileName()
            val content = files[entryName] ?: continue
            val restored = File(restoreDir, "$id-$fileName")
            restored.writeBytes(content)
            helper.setDestination(id, restored.toUri().toString())
        }
    }
}

private const val MANIFEST_ENTRY = "manifest.json"

@Serializable
private data class FileTransferBackupPayload(
    val id: Int,
    val publicKey: String,
    val fileNumber: Int,
    val fileKind: Int,
    val fileSize: Long,
    val fileName: String,
    val outgoing: Boolean,
    val progress: Long,
    val destination: String
)

@Serializable
private data class FileTransfersBackupContainer(
    val fileTransfers: List<FileTransferBackupPayload>
)

@Serializable
private data class TransferredFileManifestItem(
    val id: Int,
    val fileName: String,
    val entryName: String
)

@Serializable
private data class TransferredFileManifest(
    val files: List<TransferredFileManifestItem>
)

private fun serializeFileTransfers(fileTransfers: List<FileTransfer>): ByteArray {
    val container = FileTransfersBackupContainer(
        fileTransfers = fileTransfers.map { transfer ->
            FileTransferBackupPayload(
                id = transfer.id,
                publicKey = transfer.publicKey,
                fileNumber = transfer.fileNumber,
                fileKind = transfer.fileKind,
                fileSize = transfer.fileSize,
                fileName = transfer.fileName,
                outgoing = transfer.outgoing,
                progress = transfer.progress,
                destination = transfer.destination
            )
        }
    )
    return Json.encodeToString(container).encodeToByteArray()
}

private fun parseFileTransfers(data: ByteArray): List<FileTransfer> {
    val container = Json.decodeFromString<FileTransfersBackupContainer>(data.decodeToString())
    return container.fileTransfers.map { item ->
        FileTransfer(
            publicKey = item.publicKey,
            fileNumber = item.fileNumber,
            fileKind = item.fileKind,
            fileSize = item.fileSize,
            fileName = item.fileName,
            outgoing = item.outgoing,
            progress = item.progress,
            destination = item.destination,
        ).apply {
            id = item.id
        }
    }
}

private fun String.sanitizeZipName(): String = sanitizeFileName().ifBlank { "file" }

private const val MAX_FILENAME_LENGTH = 120

private fun String.sanitizeFileName(): String =
    replace(Regex("""[\\/:*?"<>|]"""), "_").take(MAX_FILENAME_LENGTH)
