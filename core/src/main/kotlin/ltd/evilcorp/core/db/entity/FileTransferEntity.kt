package ltd.evilcorp.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.domain.model.FT_NOT_STARTED

@Entity(tableName = "file_transfers")
data class FileTransferEntity(
    @ColumnInfo(name = "public_key")
    val publicKey: String,

    @ColumnInfo(name = "file_number")
    val fileNumber: Int,

    @ColumnInfo(name = "file_kind")
    val fileKind: Int,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "outgoing")
    val outgoing: Boolean,

    @ColumnInfo(name = "progress")
    var progress: Long = FT_NOT_STARTED,

    @ColumnInfo(name = "destination")
    var destination: String = "",
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    fun toDomain(): FileTransfer = FileTransfer(
        publicKey = publicKey,
        fileNumber = fileNumber,
        fileKind = fileKind,
        fileSize = fileSize,
        fileName = fileName,
        outgoing = outgoing,
        progress = progress,
        destination = destination
    ).apply {
        id = this@FileTransferEntity.id
    }

    companion object {
        fun fromDomain(fileTransfer: FileTransfer): FileTransferEntity = FileTransferEntity(
            publicKey = fileTransfer.publicKey,
            fileNumber = fileTransfer.fileNumber,
            fileKind = fileTransfer.fileKind,
            fileSize = fileTransfer.fileSize,
            fileName = fileTransfer.fileName,
            outgoing = fileTransfer.outgoing,
            progress = fileTransfer.progress,
            destination = fileTransfer.destination
        ).apply {
            id = fileTransfer.id
        }
    }
}
