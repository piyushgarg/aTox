package ltd.evilcorp.core.tox.runtime

import android.util.Log
import kotlin.random.Random
import ltd.evilcorp.core.model.FileKind
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.core.tox.NativeToxAv
import ltd.evilcorp.core.tox.ToxID
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.enums.ToxavCallControl
import ltd.evilcorp.core.tox.enums.ToxFileControl
import ltd.evilcorp.core.tox.save.SaveOptions
import ltd.evilcorp.core.tox.toToxtype
import ltd.evilcorp.core.tox.toToxType

private const val TAG = "ToxWrapper"

// TODO(robinlinden) Make configurable.
// https://wiki.xiph.org/Opus_Recommended_Settings
// 32 should be good enough for fullband stereo.
private const val AUDIO_BIT_RATE = 32
private const val FILE_ID_LENGTH = 32

enum class CustomPacketError {
    Success,
    Empty,
    FriendNotConnected,
    FriendNotFound,
    Invalid,
    Null,
    Sendq,
    TooLong,
}

class ToxWrapper(
    private val eventListener: ToxEventListener,
    private val avEventListener: ToxAvEventListener,
    options: SaveOptions,
) {
    private val nativeTox = NativeTox()
    private val nativeToxAv = NativeToxAv()
    private var toxPtr: Long = 0
    private var toxavPtr: Long = 0

    init {
        val sd = options.saveData
        toxPtr = nativeTox.toxNew(sd)
        if (toxPtr != 0L) {
            toxavPtr = nativeToxAv.toxavNew(toxPtr)
        }
        updateContactMapping()
    }

    private fun updateContactMapping() {
        val contacts = getContacts()
        eventListener.contactMapping = contacts
        avEventListener.contactMapping = contacts
    }

    fun bootstrap(address: String, port: Int, publicKey: ByteArray) = synchronized(this) {
        nativeTox.toxBootstrap(toxPtr, address, port, publicKey)
        nativeTox.toxAddTcpRelay(toxPtr, address, port, publicKey)
    }

    fun stop() = synchronized(this) {
        nativeToxAv.toxavKill(toxavPtr)
        nativeTox.toxKill(toxPtr)
        toxavPtr = 0
        toxPtr = 0
        Log.i(TAG, "Killed Tox")
    }

    fun iterate(): Unit = synchronized(this) { nativeTox.toxIterate(toxPtr, eventListener) }
    fun iterateAv(): Unit = synchronized(this) { nativeToxAv.toxavIterate(toxavPtr, avEventListener) }
    fun iterationInterval(): Long = synchronized(this) { nativeTox.toxIterationInterval(toxPtr).toLong() }
    fun iterationIntervalAv(): Long = synchronized(this) { nativeToxAv.toxavIterationInterval(toxavPtr).toLong() }

    fun getName(): String = synchronized(this) { String(nativeTox.toxGetName(toxPtr)) }
    fun setName(name: String) = synchronized(this) {
        nativeTox.toxSetName(toxPtr, name.toByteArray())
    }

    fun getStatusMessage(): String = synchronized(this) { String(nativeTox.toxGetStatusMessage(toxPtr)) }
    fun setStatusMessage(statusMessage: String) = synchronized(this) {
        nativeTox.toxSetStatusMessage(toxPtr, statusMessage.toByteArray())
    }

    fun getToxId() = synchronized(this) { ToxID.fromBytes(nativeTox.toxGetAddress(toxPtr)) }
    fun getPublicKey() = synchronized(this) { PublicKey.fromBytes(nativeTox.toxGetPublicKey(toxPtr)) }
    fun getNospam(): Int = synchronized(this) { nativeTox.toxGetNospam(toxPtr) }
    fun setNospam(value: Int) = synchronized(this) {
        nativeTox.toxSetNospam(toxPtr, value)
    }

    fun getSaveData() = synchronized(this) { nativeTox.toxGetSavedata(toxPtr) }

    fun addContact(toxId: ToxID, message: String) = synchronized(this) {
        nativeTox.toxAddFriend(toxPtr, toxId.bytes(), message.toByteArray())
        updateContactMapping()
    }

    fun deleteContact(pk: PublicKey) = synchronized(this) {
        Log.i(TAG, "Deleting ${pk.fingerprint()}")
        val friendNumber = nativeTox.toxFriendByPublicKey(toxPtr, pk.bytes())
        if (friendNumber != -1) {
            nativeTox.toxDeleteFriend(toxPtr, friendNumber)
        } else {
            Log.e(TAG, "Tried to delete nonexistent contact, this can happen if the database is out of sync with the Tox save")
        }
        updateContactMapping()
    }

    fun getContacts(): List<Pair<PublicKey, Int>> = synchronized(this) {
        val friendNumbers = nativeTox.toxGetFriendList(toxPtr)
        Log.i(TAG, "Loading ${friendNumbers.size} friends")
        List(friendNumbers.size) {
            Pair(PublicKey.fromBytes(nativeTox.toxGetFriendPublicKey(toxPtr, friendNumbers[it])), friendNumbers[it])
        }
    }

    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int = synchronized(this) {
        nativeTox.toxFriendSendMessage(
            toxPtr,
            contactByKey(publicKey),
            type.toToxType().ordinal,
            message.toByteArray(),
        )
    }

    fun acceptFriendRequest(pk: PublicKey) = synchronized(this) {
        try {
            nativeTox.toxAddFriendNorequest(toxPtr, pk.bytes())
            updateContactMapping()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while accepting friend request $pk: $e")
        }
    }

    fun startFileTransfer(pk: PublicKey, fileNumber: Int) = synchronized(this) {
        try {
            nativeTox.toxFileControl(toxPtr, contactByKey(pk), fileNumber, ToxFileControl.RESUME.ordinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ft ${pk.fingerprint()} $fileNumber\n$e")
        }
    }

    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) = synchronized(this) {
        try {
            nativeTox.toxFileControl(toxPtr, contactByKey(pk), fileNumber, ToxFileControl.CANCEL.ordinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ft ${pk.fingerprint()} $fileNumber\n$e")
        }
    }

    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int = synchronized(this) {
        try {
            nativeTox.toxFileSend(toxPtr, contactByKey(pk), fileKind.toToxtype(), fileSize, Random.nextBytes(FILE_ID_LENGTH), fileName.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending ft $fileName ${pk.fingerprint()}\n$e")
            -1
        }
    }

    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = synchronized(this) {
        try {
            nativeTox.toxFileSendChunk(toxPtr, contactByKey(pk), fileNo, pos, data)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending chunk $pos:${data.size} to ${pk.fingerprint()} $fileNo\n$e")
            Result.failure(e)
        }
    }

    fun setTyping(publicKey: PublicKey, typing: Boolean) = synchronized(this) {
        nativeTox.toxSetTyping(toxPtr, contactByKey(publicKey), typing)
    }

    fun getStatus() = synchronized(this) { UserStatus.entries[nativeTox.toxGetSelfUserStatus(toxPtr)] }
    fun setStatus(status: UserStatus) = synchronized(this) {
        nativeTox.toxSetSelfUserStatus(toxPtr, status.toToxType().ordinal)
    }

    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): CustomPacketError = synchronized(this) {
        try {
            nativeTox.toxFriendSendLosslessPacket(toxPtr, contactByKey(pk), packet)
            CustomPacketError.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending lossless packet: $e")
            CustomPacketError.Invalid
        }
    }

    private fun contactByKey(pk: PublicKey): Int = synchronized(this) {
        nativeTox.toxFriendByPublicKey(toxPtr, pk.bytes())
    }

    // ToxAv, probably move these.
    fun startCall(pk: PublicKey) = synchronized(this) {
        nativeToxAv.toxavCall(toxavPtr, contactByKey(pk), AUDIO_BIT_RATE, 0)
    }
    fun answerCall(pk: PublicKey) = synchronized(this) {
        nativeToxAv.toxavAnswer(toxavPtr, contactByKey(pk), AUDIO_BIT_RATE, 0)
    }
    fun endCall(pk: PublicKey) = synchronized(this) {
        nativeToxAv.toxavCallControl(toxavPtr, contactByKey(pk), ToxavCallControl.CANCEL.ordinal)
    }
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) = synchronized(this) {
        nativeToxAv.toxavAudioSendFrame(toxavPtr, contactByKey(pk), pcm, pcm.size, channels, samplingRate)
    }
}
