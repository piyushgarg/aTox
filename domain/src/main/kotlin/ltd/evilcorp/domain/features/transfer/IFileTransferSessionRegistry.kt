package ltd.evilcorp.domain.features.transfer

import ltd.evilcorp.domain.core.io.IInputStream
import ltd.evilcorp.domain.features.transfer.model.FileTransfer

@Suppress("ArrayInDataClass")
data class Chunk(val pos: Long, val data: ByteArray)

data class OutgoingFile(val inputStream: IInputStream, val unsentChunks: MutableList<Chunk>)

interface IFileTransferSessionRegistry {
    val fileTransfers: MutableList<FileTransfer>
    val outgoingFiles: MutableMap<Pair<String, Int>, OutgoingFile>
}
