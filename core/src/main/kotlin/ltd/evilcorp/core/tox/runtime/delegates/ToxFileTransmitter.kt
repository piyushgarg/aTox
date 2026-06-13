package ltd.evilcorp.core.tox.runtime.delegates

import android.util.Log
import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.enums.ToxFileControl
import ltd.evilcorp.domain.core.network.toToxtype
import kotlin.random.Random

private const val TAG = "ToxFileTransmitter"
private const val FILE_ID_LENGTH = 32

class ToxFileTransmitter(
    private val nativeTox: NativeTox,
    lock: Any,
    toxPtrProvider: () -> Long,
) : BaseToxBridge(lock, toxPtrProvider) {
    private fun contactByKey(ptr: Long, pk: PublicKey): Int {
        return nativeTox.toxFriendByPublicKey(ptr, pk.bytes())
    }

    fun startFileTransfer(pk: PublicKey, fileNumber: Int) = withTox { ptr ->
        try {
            nativeTox.toxFileControl(ptr, contactByKey(ptr, pk), fileNumber, ToxFileControl.RESUME.ordinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ft ${pk.fingerprint()} $fileNumber\n$e")
        }
    }

    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) = withTox { ptr ->
        try {
            nativeTox.toxFileControl(ptr, contactByKey(ptr, pk), fileNumber, ToxFileControl.CANCEL.ordinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ft ${pk.fingerprint()} $fileNumber\n$e")
        }
    }

    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int = withTox { ptr ->
        try {
            nativeTox.toxFileSend(
                ptr,
                contactByKey(ptr, pk),
                fileKind.toToxtype(),
                fileSize,
                Random.nextBytes(FILE_ID_LENGTH),
                fileName.toByteArray()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending ft $fileName ${pk.fingerprint()}\n$e")
            -1
        }
    }

    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = withTox { ptr ->
        try {
            nativeTox.toxFileSendChunk(ptr, contactByKey(ptr, pk), fileNo, pos, data)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending chunk $pos:${data.size} to ${pk.fingerprint()} $fileNo\n$e")
            Result.failure(e)
        }
    }
}
