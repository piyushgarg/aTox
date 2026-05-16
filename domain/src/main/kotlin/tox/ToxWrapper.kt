// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox

import android.util.Log
import ltd.evilcorp.domain.tox.enums.ToxavCallControl
import ltd.evilcorp.domain.tox.enums.ToxFileControl
import kotlin.random.Random
import ltd.evilcorp.core.vo.FileKind
import ltd.evilcorp.core.vo.MessageType
import ltd.evilcorp.core.vo.PublicKey
import ltd.evilcorp.core.vo.UserStatus

private const val TAG = "ToxWrapper"

// TODO(robinlinden) Make configurable.
// https://wiki.xiph.org/Opus_Recommended_Settings
// 32 should be good enough for fullband stereo.
private const val AUDIO_BIT_RATE = 32

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

    fun bootstrap(address: String, port: Int, publicKey: ByteArray) {
        nativeTox.toxBootstrap(toxPtr, address, port, publicKey)
        nativeTox.toxAddTcpRelay(toxPtr, address, port, publicKey)
    }

    fun stop() {
        nativeToxAv.toxavKill(toxavPtr)
        nativeTox.toxKill(toxPtr)
        toxavPtr = 0
        toxPtr = 0
        Log.i(TAG, "Killed Tox")
    }

    fun iterate(): Unit = nativeTox.toxIterate(toxPtr, eventListener)
    fun iterateAv(): Unit = nativeToxAv.toxavIterate(toxavPtr, avEventListener)
    fun iterationInterval(): Long = nativeTox.toxIterationInterval(toxPtr).toLong()
    fun iterationIntervalAv(): Long = nativeToxAv.toxavIterationInterval(toxavPtr).toLong()

    fun getName(): String = String(nativeTox.toxGetName(toxPtr))
    fun setName(name: String) {
        nativeTox.toxSetName(toxPtr, name.toByteArray())
    }

    fun getStatusMessage(): String = String(nativeTox.toxGetStatusMessage(toxPtr))
    fun setStatusMessage(statusMessage: String) {
        nativeTox.toxSetStatusMessage(toxPtr, statusMessage.toByteArray())
    }

    fun getToxId() = ToxID.fromBytes(nativeTox.toxGetAddress(toxPtr))
    fun getPublicKey() = PublicKey.fromBytes(nativeTox.toxGetPublicKey(toxPtr))
    fun getNospam(): Int = nativeTox.toxGetNospam(toxPtr)
    fun setNospam(value: Int) {
        nativeTox.toxSetNospam(toxPtr, value)
    }

    fun getSaveData() = nativeTox.toxGetSavedata(toxPtr)

    fun addContact(toxId: ToxID, message: String) {
        nativeTox.toxAddFriend(toxPtr, toxId.bytes(), message.toByteArray())
        updateContactMapping()
    }

    fun deleteContact(pk: PublicKey) {
        Log.i(TAG, "Deleting ${pk.fingerprint()}")
        val friendNumber = nativeTox.toxFriendByPublicKey(toxPtr, pk.bytes())
        if (friendNumber != -1) {
            nativeTox.toxDeleteFriend(toxPtr, friendNumber)
        } else {
            Log.e(TAG, "Tried to delete nonexistent contact, this can happen if the database is out of sync with the Tox save")
        }
        updateContactMapping()
    }

    fun getContacts(): List<Pair<PublicKey, Int>> {
        val friendNumbers = nativeTox.toxGetFriendList(toxPtr)
        Log.i(TAG, "Loading ${friendNumbers.size} friends")
        return List(friendNumbers.size) {
            Pair(PublicKey.fromBytes(nativeTox.toxGetFriendPublicKey(toxPtr, friendNumbers[it])), friendNumbers[it])
        }
    }

    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int = nativeTox.toxFriendSendMessage(
        toxPtr,
        contactByKey(publicKey),
        type.toToxType().ordinal,
        message.toByteArray(),
    )

    fun acceptFriendRequest(pk: PublicKey) {
        try {
            nativeTox.toxAddFriendNorequest(toxPtr, pk.bytes())
            updateContactMapping()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while accepting friend request $pk: $e")
        }
    }

    fun startFileTransfer(pk: PublicKey, fileNumber: Int) {
        try {
            nativeTox.toxFileControl(toxPtr, contactByKey(pk), fileNumber, ToxFileControl.RESUME.ordinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ft ${pk.fingerprint()} $fileNumber\n$e")
        }
    }

    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {
        try {
            nativeTox.toxFileControl(toxPtr, contactByKey(pk), fileNumber, ToxFileControl.CANCEL.ordinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ft ${pk.fingerprint()} $fileNumber\n$e")
        }
    }

    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int {
        return try {
            nativeTox.toxFileSend(toxPtr, contactByKey(pk), fileKind.toToxtype(), fileSize, Random.nextBytes(32), fileName.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending ft $fileName ${pk.fingerprint()}\n$e")
            -1
        }
    }

    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = try {
        nativeTox.toxFileSendChunk(toxPtr, contactByKey(pk), fileNo, pos, data)
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error sending chunk $pos:${data.size} to ${pk.fingerprint()} $fileNo\n$e")
        Result.failure(e)
    }

    fun setTyping(publicKey: PublicKey, typing: Boolean) = nativeTox.toxSetTyping(toxPtr, contactByKey(publicKey), typing)

    fun getStatus() = UserStatus.entries[nativeTox.toxGetSelfUserStatus(toxPtr)]
    fun setStatus(status: UserStatus) {
        nativeTox.toxSetSelfUserStatus(toxPtr, status.toToxType().ordinal)
    }

    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): CustomPacketError = try {
        nativeTox.toxFriendSendLosslessPacket(toxPtr, contactByKey(pk), packet)
        CustomPacketError.Success
    } catch (e: Exception) {
        Log.e(TAG, "Error sending lossless packet: $e")
        CustomPacketError.Invalid
    }

    private fun contactByKey(pk: PublicKey): Int = nativeTox.toxFriendByPublicKey(toxPtr, pk.bytes())

    // ToxAv, probably move these.
    fun startCall(pk: PublicKey) = nativeToxAv.toxavCall(toxavPtr, contactByKey(pk), AUDIO_BIT_RATE, 0)
    fun answerCall(pk: PublicKey) = nativeToxAv.toxavAnswer(toxavPtr, contactByKey(pk), AUDIO_BIT_RATE, 0)
    fun endCall(pk: PublicKey) = nativeToxAv.toxavCallControl(toxavPtr, contactByKey(pk), ToxavCallControl.CANCEL.ordinal)
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) =
        nativeToxAv.toxavAudioSendFrame(toxavPtr, contactByKey(pk), pcm, pcm.size, channels, samplingRate)
}
