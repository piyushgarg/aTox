package ltd.evilcorp.core.tox.listener

import ltd.evilcorp.domain.core.network.enums.ToxFileControl

class ToxFileEventDispatcher(private val listener: ToxEventListener) {

    fun onFileRecv(publicKey: String, fileNo: Int, kind: Int, fileSize: Long, filename: ByteArray) {
        listener.fileRecvHandler(
            publicKey,
            fileNo,
            kind,
            fileSize,
            String(filename)
        )
    }

    fun onFileRecvControl(publicKey: String, fileNo: Int, control: Int) {
        listener.fileRecvControlHandler(
            publicKey,
            fileNo,
            ToxFileControl.fromInt(control)
        )
    }

    fun onFileRecvChunk(publicKey: String, fileNo: Int, position: Long, data: ByteArray) {
        listener.fileRecvChunkHandler(
            publicKey,
            fileNo,
            position,
            data
        )
    }

    fun onFileChunkRequest(publicKey: String, fileNo: Int, position: Long, length: Int) {
        listener.fileChunkRequestHandler(
            publicKey,
            fileNo,
            position,
            length
        )
    }
}
