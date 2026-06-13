package ltd.evilcorp.domain.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository

class FakeFileTransferRepository : IFileTransferRepository {
    private val transfers = MutableStateFlow<Map<Int, FileTransfer>>(emptyMap())

    override suspend fun add(ft: FileTransfer): Long {
        val id = ft.id
        transfers.value = transfers.value + (id to ft)
        return id.toLong()
    }

    override suspend fun delete(id: Int) {
        transfers.value = transfers.value - id
    }

    override fun get(publicKey: String): Flow<List<FileTransfer>> {
        return transfers.map { map -> map.values.filter { it.publicKey == publicKey } }
    }

    override fun get(id: Int): Flow<FileTransfer> {
        return transfers.map { it[id] }.filterNotNull()
    }

    override suspend fun setDestination(id: Int, destination: String) {
        updateField(id) { it.copy(destination = destination) }
    }

    override suspend fun updateProgress(id: Int, progress: Long) {
        updateField(id) { it.copy(progress = progress) }
    }

    override suspend fun resetTransientData() {
        transfers.value = emptyMap()
    }

    private fun updateField(id: Int, update: (FileTransfer) -> FileTransfer) {
        val current = transfers.value[id]
        if (current != null) {
            transfers.value = transfers.value + (id to update(current))
        }
    }
}
