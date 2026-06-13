package ltd.evilcorp.domain.fakes

import ltd.evilcorp.domain.features.transfer.IFileTransferSessionRegistry
import ltd.evilcorp.domain.features.transfer.OutgoingFile
import ltd.evilcorp.domain.features.transfer.model.FileTransfer

class FakeFileTransferSessionRegistry : IFileTransferSessionRegistry {
    override val fileTransfers = mutableListOf<FileTransfer>()
    override val outgoingFiles = mutableMapOf<Pair<String, Int>, OutgoingFile>()
}
