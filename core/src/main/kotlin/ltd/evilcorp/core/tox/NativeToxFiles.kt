package ltd.evilcorp.core.tox

class NativeToxFiles(private val nativeTox: NativeTox) {
    fun toxFileControl(tox: Long, friendNumber: Int, fileNumber: Int, control: Int) =
        nativeTox.toxFileControl(tox, friendNumber, fileNumber, control)

    fun toxFileSend(tox: Long, friendNumber: Int, kind: Int, fileSize: Long, fileId: ByteArray, filename: ByteArray): Int =
        nativeTox.toxFileSend(tox, friendNumber, kind, fileSize, fileId, filename)

    fun toxFileSendChunk(tox: Long, friendNumber: Int, fileNumber: Int, position: Long, data: ByteArray) =
        nativeTox.toxFileSendChunk(tox, friendNumber, fileNumber, position, data)

    fun toxFileGetFileId(tox: Long, friendNumber: Int, fileNumber: Int): ByteArray =
        nativeTox.toxFileGetFileId(tox, friendNumber, fileNumber)
}
