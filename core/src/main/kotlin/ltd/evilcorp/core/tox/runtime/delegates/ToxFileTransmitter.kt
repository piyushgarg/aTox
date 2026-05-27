package ltd.evilcorp.core.tox.runtime.delegates

import android.util.Log
import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.domain.model.FileKind
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.tox.enums.ToxFileControl
import ltd.evilcorp.domain.tox.toToxtype
import kotlin.random.Random

private const val TAG = "ToxFileTransmitter"
private const val FILE_ID_LENGTH = 32

class ToxFileTransmitter(
    private val nativeTox: NativeTox,
    private val toxPtrProvider: () -> Long,
) {
    private fun contactByKey(pk: PublicKey): Int {
        return nativeTox.toxFriendByPublicKey(toxPtrProvider(), pk.bytes())
    }

    fun startFileTransfer(pk: PublicKey, fileNumber: Int) = synchronized(this) {
        try {
            nativeTox.toxFileControl(toxPtrProvider(), contactByKey(pk), fileNumber, ToxFileControl.RESUME.ordinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ft ${pk.fingerprint()} $fileNumber\n$e")
        }
    }

    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) = synchronized(this) {
        try {
            nativeTox.toxFileControl(toxPtrProvider(), contactByKey(pk), fileNumber, ToxFileControl.CANCEL.ordinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ft ${pk.fingerprint()} $fileNumber\n$e")
        }
    }

    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int = synchronized(this) {
        try {
            nativeTox.toxFileSend(
                toxPtrProvider(),
                contactByKey(pk),
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

    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = synchronized(this) {
        try {
            nativeTox.toxFileSendChunk(toxPtrProvider(), contactByKey(pk), fileNo, pos, data)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending chunk $pos:${data.size} to ${pk.fingerprint()} $fileNo\n$e")
            Result.failure(e)
        }
    }
}
