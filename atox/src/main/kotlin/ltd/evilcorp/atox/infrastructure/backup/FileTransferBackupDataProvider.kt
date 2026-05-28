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
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.features.backup.repository.IFileTransferBackupHelper
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import org.json.JSONArray
import org.json.JSONObject

class FileTransferHistoryBackupDataProvider @Inject constructor(
    private val helper: IFileTransferBackupHelper,
) : IBackupDataProvider {
    override val id: String = "file_transfer_history"
    override val displayNameRes: Int = R.string.backup_module_file_transfer_history
    override val descriptionRes: Int = R.string.backup_module_file_transfer_history_description

    override suspend fun serialize(): ByteArray = serializeFileTransfers(helper.serializeFileTransfers())

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

    override suspend fun serialize(): ByteArray {
        val manifest = JSONArray()
        val transfers = helper.serializeFileTransfers()
            .filter { it.destination.isNotBlank() }

        return ByteArrayOutputStream().use { bytes ->
            ZipOutputStream(bytes).use { zip ->
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
                        manifest.put(JSONObject().apply {
                            put("id", transfer.id)
                            put("fileName", transfer.fileName)
                            put("entryName", entryName)
                        })
                    }
                }

                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(JSONObject().put("files", manifest).toString().encodeToByteArray())
                zip.closeEntry()
            }
            bytes.toByteArray()
        }
    }

    override suspend fun deserialize(data: ByteArray) {
        val files = mutableMapOf<String, ByteArray>()
        var manifest = JSONArray()

        ZipInputStream(ByteArrayInputStream(data)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val bytes = zip.readBytes()
                if (entry.name == MANIFEST_ENTRY) {
                    manifest = JSONObject(bytes.decodeToString()).getJSONArray("files")
                } else {
                    files[entry.name] = bytes
                }
                zip.closeEntry()
            }
        }

        val restoreDir = File(context.filesDir, "ft/restored").apply { mkdirs() }
        for (index in 0 until manifest.length()) {
            val item = manifest.getJSONObject(index)
            val id = item.getInt("id")
            val entryName = item.getString("entryName")
            val fileName = item.getString("fileName").sanitizeFileName()
            val content = files[entryName] ?: continue
            val restored = File(restoreDir, "$id-$fileName")
            restored.writeBytes(content)
            helper.setDestination(id, restored.toUri().toString())
        }
    }
}

private const val MANIFEST_ENTRY = "manifest.json"

private fun serializeFileTransfers(fileTransfers: List<FileTransfer>): ByteArray {
    val entries = JSONArray()
    fileTransfers.forEach { transfer ->
        entries.put(JSONObject().apply {
            put("id", transfer.id)
            put("publicKey", transfer.publicKey)
            put("fileNumber", transfer.fileNumber)
            put("fileKind", transfer.fileKind)
            put("fileSize", transfer.fileSize)
            put("fileName", transfer.fileName)
            put("outgoing", transfer.outgoing)
            put("progress", transfer.progress)
            put("destination", transfer.destination)
        })
    }
    return JSONObject().put("fileTransfers", entries).toString().encodeToByteArray()
}

private fun parseFileTransfers(data: ByteArray): List<FileTransfer> {
    val entries = JSONObject(data.decodeToString()).getJSONArray("fileTransfers")
    return buildList {
        for (index in 0 until entries.length()) {
            val item = entries.getJSONObject(index)
            add(FileTransfer(
                publicKey = item.getString("publicKey"),
                fileNumber = item.getInt("fileNumber"),
                fileKind = item.getInt("fileKind"),
                fileSize = item.getLong("fileSize"),
                fileName = item.getString("fileName"),
                outgoing = item.getBoolean("outgoing"),
                progress = item.getLong("progress"),
                destination = item.optString("destination"),
            ).apply {
                id = item.getInt("id")
            })
        }
    }
}

private fun String.sanitizeZipName(): String = sanitizeFileName().ifBlank { "file" }

private const val MAX_FILENAME_LENGTH = 120

private fun String.sanitizeFileName(): String =
    replace(Regex("""[\\/:*?"<>|]"""), "_").take(MAX_FILENAME_LENGTH)
