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
import ltd.evilcorp.core.tox.enums.ToxMessageType
import ltd.evilcorp.core.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.core.tox.enums.ToxGroupRole
import ltd.evilcorp.core.tox.save.SaveOptions
import ltd.evilcorp.core.tox.toToxtype
import ltd.evilcorp.core.tox.toToxType

private const val TAG = "ToxWrapper"

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

/**
 * Высокоуровневая потокобезопасная обертка над нативными библиотеками NativeTox и NativeToxAv.
 * Управляет жизненным циклом сессии Tox, вызовами сетевых функций, файловыми трансферами и AV-звонками.
 */
class ToxWrapper(
    private val eventListener: ToxEventListener,
    private val avEventListener: ToxAvEventListener,
    options: SaveOptions,
) {
    private val nativeTox = NativeTox()
    private val nativeToxAv = NativeToxAv()
    private var toxPtr: Long = 0
    private var toxavPtr: Long = 0

    /**
     * Динамически настраиваемый битрейт аудио Opus (в kbps).
     * Рекомендуемое значение: 32 kbps (достаточно для качественного полнополосного стерео-звука).
     */
    @Volatile
    var audioBitrate: Int = 32

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
        nativeToxAv.toxavCall(toxavPtr, contactByKey(pk), audioBitrate, 0)
    }
    fun answerCall(pk: PublicKey) = synchronized(this) {
        nativeToxAv.toxavAnswer(toxavPtr, contactByKey(pk), audioBitrate, 0)
    }
    fun endCall(pk: PublicKey) = synchronized(this) {
        nativeToxAv.toxavCallControl(toxavPtr, contactByKey(pk), ToxavCallControl.CANCEL.ordinal)
    }
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) = synchronized(this) {
        nativeToxAv.toxavAudioSendFrame(toxavPtr, contactByKey(pk), pcm, pcm.size, channels, samplingRate)
    }

    // NGC Groups / Modern Conferences
    fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int = synchronized(this) {
        nativeTox.toxGroupNew(toxPtr, privacyState.value, groupName, selfName)
    }

    fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int = synchronized(this) {
        nativeTox.toxGroupJoin(toxPtr, friendNo, inviteData, selfName, password)
    }

    fun groupLeave(groupNumber: Int): Boolean = synchronized(this) {
        nativeTox.toxGroupLeave(toxPtr, groupNumber)
    }

    fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int = synchronized(this) {
        nativeTox.toxGroupSendMessage(toxPtr, groupNumber, type.ordinal, message)
    }

    fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = synchronized(this) {
        nativeTox.toxGroupSetTopic(toxPtr, groupNumber, topic)
    }

    fun groupGetTopic(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetTopic(toxPtr, groupNumber)
    }

    fun groupGetName(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetName(toxPtr, groupNumber)
    }

    fun groupGetChatId(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetChatId(toxPtr, groupNumber)
    }

    fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = synchronized(this) {
        nativeTox.toxGroupSetPassword(toxPtr, groupNumber, password)
    }

    fun groupGetPassword(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetPassword(toxPtr, groupNumber)
    }

    fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupPeerGetName(toxPtr, groupNumber, peerId)
    }

    fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupPeerGetPublicKey(toxPtr, groupNumber, peerId)
    }

    fun groupSelfGetPeerId(groupNumber: Int): Int = synchronized(this) {
        nativeTox.toxGroupSelfGetPeerId(toxPtr, groupNumber)
    }

    fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = synchronized(this) {
        ToxGroupRole.fromInt(nativeTox.toxGroupSelfGetRole(toxPtr, groupNumber))
    }

    // Modern Group Call (ToxAV)
    fun groupavAdd(): Int = synchronized(this) {
        nativeToxAv.toxavAddAvGroupchat(toxPtr)
    }

    fun groupavJoin(groupNumber: Int): Int = synchronized(this) {
        nativeToxAv.toxavJoinAvGroupchat(toxPtr, groupNumber)
    }

    fun groupavSendAudio(groupNumber: Int, pcm: ShortArray, channels: Int, samplingRate: Int): Int = synchronized(this) {
        nativeToxAv.toxavGroupSendAudio(toxPtr, groupNumber, pcm, pcm.size, channels, samplingRate)
    }

    fun groupavEnableAudio(groupNumber: Int): Int = synchronized(this) {
        nativeToxAv.toxavGroupchatEnableAv(toxPtr, groupNumber)
    }

    fun groupavDisableAudio(groupNumber: Int): Int = synchronized(this) {
        nativeToxAv.toxavGroupchatDisableAv(toxPtr, groupNumber)
    }

    fun groupavIsEnabled(groupNumber: Int): Boolean = synchronized(this) {
        nativeToxAv.toxavGroupchatAvEnabled(toxPtr, groupNumber)
    }
}
