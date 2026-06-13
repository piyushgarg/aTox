package ltd.evilcorp.core.platform.storage

import ltd.evilcorp.domain.features.transfer.IFileTransferSessionRegistry
import ltd.evilcorp.domain.features.transfer.OutgoingFile
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferSessionRegistryImpl @Inject constructor() : IFileTransferSessionRegistry {
    override val fileTransfers: MutableList<FileTransfer> = CopyOnWriteArrayList()
    override val outgoingFiles: MutableMap<Pair<String, Int>, OutgoingFile> = ConcurrentHashMap()
}
