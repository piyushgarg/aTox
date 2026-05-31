package ltd.evilcorp.core.db.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.core.db.entity.FileTransferEntity

class FakeFileTransferDao : FileTransferDao {
    private val transfers = MutableStateFlow<List<FileTransferEntity>>(emptyList())
    private var nextId = 1

    override suspend fun save(fileTransfer: FileTransferEntity): Long {
        if (fileTransfer.id == 0) {
            fileTransfer.id = nextId++
        }
        transfers.value = transfers.value.filter { it.id != fileTransfer.id } + fileTransfer
        return fileTransfer.id.toLong()
    }

    override suspend fun saveAll(fileTransfers: List<FileTransferEntity>) {
        val list = transfers.value.toMutableList()
        fileTransfers.forEach { ft ->
            if (ft.id == 0) {
                ft.id = nextId++
            }
            list.removeAll { it.id == ft.id }
            list.add(ft)
        }
        transfers.value = list
    }

    override suspend fun delete(id: Int) {
        transfers.value = transfers.value.filter { it.id != id }
    }

    override fun load(publicKey: String): Flow<List<FileTransferEntity>> {
        return transfers.map { list -> list.filter { it.publicKey == publicKey } }
    }

    override fun load(id: Int): Flow<FileTransferEntity> {
        return transfers.map { list -> list.first { it.id == id } }
    }

    override suspend fun loadAllBlocking(): List<FileTransferEntity> {
        return transfers.value
    }

    override suspend fun updateProgress(id: Int, progress: Long, rejected: Long) {
        transfers.value = transfers.value.map { ft ->
            if (ft.id == id && ft.progress != rejected) {
                ft.copy().apply {
                    this.id = ft.id
                    this.progress = progress
                }
            } else {
                ft
            }
        }
    }

    override suspend fun setDestination(id: Int, destination: String) {
        transfers.value = transfers.value.map { ft ->
            if (ft.id == id) {
                ft.copy().apply {
                    this.id = ft.id
                    this.destination = destination
                }
            } else {
                ft
            }
        }
    }

    override suspend fun resetTransientData(progress: Long) {
        transfers.value = transfers.value.map { ft ->
            if (ft.progress < ft.fileSize) {
                ft.copy().apply {
                    this.id = ft.id
                    this.progress = progress
                }
            } else {
                ft
            }
        }
    }
}
