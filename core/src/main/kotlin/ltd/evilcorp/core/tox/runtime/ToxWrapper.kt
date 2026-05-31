package ltd.evilcorp.core.tox.runtime

import android.util.Log
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.core.tox.NativeToxAv
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.core.network.toToxType
import ltd.evilcorp.core.tox.runtime.delegates.ToxFileTransmitter
import ltd.evilcorp.core.tox.runtime.delegates.ToxAudioVideoBridge
import ltd.evilcorp.core.tox.runtime.delegates.ToxGroupBridge

private const val TAG = "ToxWrapper"

/**
 * Thread-safe wrapper over native libraries NativeTox and NativeToxAv.
 * Manages the lifecycle of Tox session, file transfers, and AV calls.
 */
class ToxWrapper(
    private val eventListener: ToxEventListener,
    private val avEventListener: ToxAvEventListener,
    options: SaveOptions,
) : AutoCloseable {
    private val jniLock = Any()

    private val nativeTox = NativeTox()
    private val nativeToxAv = NativeToxAv()

    @Volatile private var toxPtr: Long = 0
    @Volatile private var toxavPtr: Long = 0

    val fileTransmitter = ToxFileTransmitter(nativeTox, jniLock) { toxPtr }
    val audioVideoBridge = ToxAudioVideoBridge(nativeTox, nativeToxAv, jniLock, { toxPtr }, { toxavPtr })
    val groupBridge = ToxGroupBridge(nativeTox, nativeToxAv, jniLock) { toxPtr }

    @Volatile var audioBitrate: Int = 32

    init {
        val sd = options.saveData
        toxPtr = nativeTox.toxNewWithOptions(
            savedata = sd,
            ipv6Enabled = true,
            udpEnabled = options.udpEnabled,
            localDiscoveryEnabled = true,
            proxyType = options.proxyType.ordinal,
            proxyHost = options.proxyAddress.ifEmpty { null },
            proxyPort = options.proxyPort
        )
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

    // Connects to a public DHT node and registers a TCP relay.
    fun bootstrap(address: String, port: Int, publicKey: ByteArray) = synchronized(jniLock) {
        nativeTox.toxBootstrap(toxPtr, address, port, publicKey)
        nativeTox.toxAddTcpRelay(toxPtr, address, port, publicKey)
    }

    // Stops native core sessions and frees native memory.
    fun stop() = synchronized(jniLock) {
        nativeToxAv.toxavKill(toxavPtr)
        nativeTox.toxKill(toxPtr)
        toxavPtr = 0
        toxPtr = 0
        Log.i(TAG, "Killed Tox")
    }

    override fun close() {
        stop()
    }

    @Suppress("DEPRECATION")
    protected fun finalize() {
        try {
            if (toxPtr != 0L || toxavPtr != 0L) {
                Log.w(TAG, "ToxWrapper was garbage collected without calling stop() / close()! Freeing native memory to avoid leak.")
                stop()
            }
        } finally {
            // JVM finalize fallback
        }
    }

    fun iterate(): Unit = synchronized(jniLock) { nativeTox.toxIterate(toxPtr, eventListener) }
    fun iterateAv(): Unit = synchronized(jniLock) { nativeToxAv.toxavIterate(toxavPtr, avEventListener) }

    fun iterationInterval(): Long = synchronized(jniLock) { nativeTox.toxIterationInterval(toxPtr).toLong() }
    fun iterationIntervalAv(): Long = synchronized(jniLock) { nativeToxAv.toxavIterationInterval(toxavPtr).toLong() }

    fun getName(): String = synchronized(jniLock) { String(nativeTox.toxGetName(toxPtr)) }
    fun setName(name: String) = synchronized(jniLock) { nativeTox.toxSetName(toxPtr, name.toByteArray()) }

    fun getStatusMessage(): String = synchronized(jniLock) { String(nativeTox.toxGetStatusMessage(toxPtr)) }
    fun setStatusMessage(statusMessage: String) = synchronized(jniLock) {
        nativeTox.toxSetStatusMessage(toxPtr, statusMessage.toByteArray())
    }

    fun getToxId() = synchronized(jniLock) { ToxID.fromBytes(nativeTox.toxGetAddress(toxPtr)) }
    fun getPublicKey() = synchronized(jniLock) { PublicKey.fromBytes(nativeTox.toxGetPublicKey(toxPtr)) }

    fun getNospam(): Int = synchronized(jniLock) { nativeTox.toxGetNospam(toxPtr) }
    fun setNospam(value: Int) = synchronized(jniLock) { nativeTox.toxSetNospam(toxPtr, value) }

    fun getSaveData() = synchronized(jniLock) { nativeTox.toxGetSavedata(toxPtr) }

    // Adds a new contact by their full Tox ID and sends a friend request.
    fun addContact(toxId: ToxID, message: String) = synchronized(jniLock) {
        nativeTox.toxAddFriend(toxPtr, toxId.bytes(), message.toByteArray())
        updateContactMapping()
    }

    // Deletes a contact from the contact list by their public key.
    fun deleteContact(pk: PublicKey) = synchronized(jniLock) {
        Log.i(TAG, "Deleting ${pk.fingerprint()}")
        val friendNumber = nativeTox.toxFriendByPublicKey(toxPtr, pk.bytes())
        if (friendNumber != -1) {
            nativeTox.toxDeleteFriend(toxPtr, friendNumber)
        } else {
            Log.e(TAG, "Tried to delete nonexistent contact")
        }
        updateContactMapping()
    }

    // Returns a list of all friends as pairs of (PublicKey, native ID).
    fun getContacts(): List<Pair<PublicKey, Int>> = synchronized(jniLock) {
        val friendNumbers = nativeTox.toxGetFriendList(toxPtr)
        Log.i(TAG, "Loading ${friendNumbers.size} friends")
        List(friendNumbers.size) {
            Pair(PublicKey.fromBytes(nativeTox.toxGetFriendPublicKey(toxPtr, friendNumbers[it])), friendNumbers[it])
        }
    }

    fun getFriendPublicKey(friendNumber: Int): ByteArray? = synchronized(jniLock) {
        try {
            nativeTox.toxGetFriendPublicKey(toxPtr, friendNumber)
        } catch (e: Exception) {
            null
        }
    }

    // Sends a private text or action message to a friend.
    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int = synchronized(jniLock) {
        nativeTox.toxFriendSendMessage(
            toxPtr,
            contactByKey(publicKey),
            type.toToxType().ordinal,
            message.toByteArray(),
        )
    }

    // Accepts an incoming friend request and adds the contact to the list.
    fun acceptFriendRequest(pk: PublicKey) = synchronized(jniLock) {
        try {
            nativeTox.toxAddFriendNorequest(toxPtr, pk.bytes())
            updateContactMapping()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while accepting friend request $pk: $e")
        }
    }

    // Adds a friend to the list without sending a request.
    fun addFriendNoRequest(pk: PublicKey): Int = synchronized(jniLock) {
        try {
            val result = nativeTox.toxAddFriendNorequest(toxPtr, pk.bytes())
            if (result >= 0) {
                updateContactMapping()
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception while adding friend norequest $pk: $e")
            -1
        }
    }

    // File transfer delegation methods
    fun startFileTransfer(pk: PublicKey, fileNumber: Int) = fileTransmitter.startFileTransfer(pk, fileNumber)
    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) = fileTransmitter.stopFileTransfer(pk, fileNumber)
    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int =
        fileTransmitter.sendFile(pk, fileKind, fileSize, fileName)
    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> =
        fileTransmitter.sendFileChunk(pk, fileNo, pos, data)

    // Sets friend typing notification status.
    fun setTyping(publicKey: PublicKey, typing: Boolean) = synchronized(jniLock) {
        nativeTox.toxSetTyping(toxPtr, contactByKey(publicKey), typing)
    }

    fun getStatus() = synchronized(jniLock) { UserStatus.entries[nativeTox.toxGetSelfUserStatus(toxPtr)] }
    fun setStatus(status: UserStatus) = synchronized(jniLock) {
        nativeTox.toxSetSelfUserStatus(toxPtr, status.toToxType().ordinal)
    }

    // Sends a reliable lossless custom data packet to a friend.
    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): CustomPacketError = synchronized(jniLock) {
        try {
            nativeTox.toxFriendSendLosslessPacket(toxPtr, contactByKey(pk), packet)
            CustomPacketError.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending lossless packet: $e")
            CustomPacketError.Invalid
        }
    }

    // Sends an unreliable lossy custom data packet to a friend.
    fun sendLossyPacket(pk: PublicKey, data: ByteArray): CustomPacketError = synchronized(jniLock) {
        try {
            nativeTox.toxFriendSendLossyPacket(toxPtr, contactByKey(pk), data)
            CustomPacketError.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending lossy packet: $e")
            CustomPacketError.Invalid
        }
    }

    fun selfGetSecretKey(): ByteArray = synchronized(jniLock) { nativeTox.toxSelfGetSecretKey(toxPtr) }
    fun selfGetUdpPort(): Int = synchronized(jniLock) { nativeTox.toxSelfGetUdpPort(toxPtr) }
    fun selfGetTcpPort(): Int = synchronized(jniLock) { nativeTox.toxSelfGetTcpPort(toxPtr) }
    fun selfGetDhtId(): ByteArray = synchronized(jniLock) { nativeTox.toxSelfGetDhtId(toxPtr) }

    fun friendGetLastOnline(pk: PublicKey): Long = synchronized(jniLock) {
        nativeTox.toxFriendGetLastOnline(toxPtr, contactByKey(pk))
    }

    fun friendGetTyping(pk: PublicKey): Boolean = synchronized(jniLock) {
        nativeTox.toxFriendGetTyping(toxPtr, contactByKey(pk))
    }

    private fun contactByKey(pk: PublicKey): Int = synchronized(jniLock) {
        nativeTox.toxFriendByPublicKey(toxPtr, pk.bytes())
    }

    fun getFriendNumberByPublicKey(pk: PublicKey): Int = synchronized(jniLock) {
        nativeTox.toxFriendByPublicKey(toxPtr, pk.bytes())
    }

    // Audio-Video call delegation methods
    fun startCall(pk: PublicKey) = audioVideoBridge.startCall(pk, audioBitrate)
    fun answerCall(pk: PublicKey) = audioVideoBridge.answerCall(pk, audioBitrate)
    fun endCall(pk: PublicKey) = audioVideoBridge.endCall(pk)
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) =
        audioVideoBridge.sendAudio(pk, pcm, channels, samplingRate)
    fun sendVideoFrame(pk: PublicKey, width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray): Boolean =
        audioVideoBridge.sendVideoFrame(pk, width, height, y, u, v)
    fun audioSetBitRate(pk: PublicKey, bitrate: Int): Boolean = audioVideoBridge.audioSetBitRate(pk, bitrate)
    fun videoSetBitRate(pk: PublicKey, bitrate: Int): Boolean = audioVideoBridge.videoSetBitRate(pk, bitrate)

    // NGC group conference delegation methods
    fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int =
        groupBridge.groupNew(privacyState, groupName, selfName)
    fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int =
        groupBridge.groupJoin(friendNo, inviteData, selfName, password)
    fun groupLeave(groupNumber: Int): Boolean = groupBridge.groupLeave(groupNumber)
    fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int =
        groupBridge.groupSendMessage(groupNumber, type, message)
    fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = groupBridge.groupSetTopic(groupNumber, topic)
    fun groupGetTopic(groupNumber: Int): ByteArray? = groupBridge.groupGetTopic(groupNumber)
    fun groupGetName(groupNumber: Int): ByteArray? = groupBridge.groupGetName(groupNumber)
    fun groupGetChatId(groupNumber: Int): ByteArray? = groupBridge.groupGetChatId(groupNumber)
    fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean =
        groupBridge.groupSetPassword(groupNumber, password)
    fun groupGetPassword(groupNumber: Int): ByteArray? = groupBridge.groupGetPassword(groupNumber)
    fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = groupBridge.groupPeerGetName(groupNumber, peerId)
    fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? =
        groupBridge.groupPeerGetPublicKey(groupNumber, peerId)
    fun groupSelfGetPeerId(groupNumber: Int): Int = groupBridge.groupSelfGetPeerId(groupNumber)
    fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = groupBridge.groupSelfGetRole(groupNumber)
    fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean =
        groupBridge.groupInviteSend(groupNumber, friendNumber)
    fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int =
        groupBridge.groupJoinDirect(chatId, selfName, password)
    fun groupReconnect(groupNumber: Int): Boolean = groupBridge.groupReconnect(groupNumber)
    fun groupGetChatlist(): IntArray = groupBridge.groupGetChatlist()
      // Legacy conference/groupav delegation methods
    fun groupavAdd(): Int = groupBridge.groupavAdd()
    fun groupavJoin(groupNumber: Int): Int = groupBridge.groupavJoin(groupNumber)
    fun groupavSendAudio(groupNumber: Int, pcm: ShortArray, channels: Int, samplingRate: Int): Int =
        groupBridge.groupavSendAudio(groupNumber, pcm, channels, samplingRate)
    fun groupavEnableAudio(groupNumber: Int): Int = groupBridge.groupavEnableAudio(groupNumber)
    fun groupavDisableAudio(groupNumber: Int): Int = groupBridge.groupavDisableAudio(groupNumber)
    fun groupavIsEnabled(groupNumber: Int): Boolean = groupBridge.groupavIsEnabled(groupNumber)
}
