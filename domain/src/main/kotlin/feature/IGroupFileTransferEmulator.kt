package ltd.evilcorp.domain.feature

interface IGroupFileTransferEmulator {
    suspend fun emulateDownload(
        id: Int,
        fileName: String,
        fileSize: Long,
        onProgress: suspend (Long) -> Unit
    ): ByteArray?
}
