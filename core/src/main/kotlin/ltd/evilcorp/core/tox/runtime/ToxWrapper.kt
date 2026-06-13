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
private const val DEFAULT_AV_ITERATION_INTERVAL_MS = 50L

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
        var ptr = nativeTox.toxNewWithOptions(
            savedata = sd,
            ipv6Enabled = true,
            udpEnabled = options.udpEnabled,
            localDiscoveryEnabled = true,
            proxyType = options.proxyType.ordinal,
            proxyHost = options.proxyAddress.ifEmpty { null },
            proxyPort = options.proxyPort
        )
        if (ptr == 0L) {
            Log.w(TAG, "Failed to initialize Tox with IPv6 enabled. Retrying with IPv6 disabled.")
            ptr = nativeTox.toxNewWithOptions(
                savedata = sd,
                ipv6Enabled = false,
                udpEnabled = options.udpEnabled,
                localDiscoveryEnabled = true,
                proxyType = options.proxyType.ordinal,
                proxyHost = options.proxyAddress.ifEmpty { null },
                proxyPort = options.proxyPort
            )
        }
        check(ptr != 0L) { "Failed to initialize Tox Core: Native pointer is null." }
        toxPtr = ptr
        toxavPtr = nativeToxAv.toxavNew(toxPtr)
        updateContactMapping()
    }

    private inline fun <T> withTox(block: (Long) -> T): T = synchronized(jniLock) {
        check(toxPtr != 0L) { "Tox native pointer is null. Tox session has been stopped." }
        block(toxPtr)
    }

    private fun updateContactMapping() {
        val contacts = getContacts()
        eventListener.contactMapping = contacts
        avEventListener.contactMapping = contacts
    }

    // Connects to a public DHT node and registers a TCP relay.
    fun bootstrap(address: String, port: Int, publicKey: ByteArray) = withTox { ptr ->
        nativeTox.toxBootstrap(ptr, address, port, publicKey)
        nativeTox.toxAddTcpRelay(ptr, address, port, publicKey)
    }

    // Stops native core sessions and frees native memory.
    fun stop() = synchronized(jniLock) {
        if (toxavPtr != 0L) {
            nativeToxAv.toxavKill(toxavPtr)
            toxavPtr = 0
        }
        if (toxPtr != 0L) {
            nativeTox.toxKill(toxPtr)
            toxPtr = 0
        }
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

    fun iterate(): Unit = withTox { ptr -> nativeTox.toxIterate(ptr, eventListener) }
    fun iterateAv(): Unit = synchronized(jniLock) {
        if (toxavPtr != 0L) {
            nativeToxAv.toxavIterate(toxavPtr, avEventListener)
        }
    }

    fun iterationInterval(): Long = withTox { ptr -> nativeTox.toxIterationInterval(ptr).toLong() }
    fun iterationIntervalAv(): Long = synchronized(jniLock) {
        if (toxavPtr != 0L) nativeToxAv.toxavIterationInterval(toxavPtr).toLong() else DEFAULT_AV_ITERATION_INTERVAL_MS
    }

    fun getName(): String = withTox { ptr -> String(nativeTox.toxGetName(ptr)) }
    fun setName(name: String) = withTox { ptr -> nativeTox.toxSetName(ptr, name.toByteArray()) }

    fun getStatusMessage(): String = withTox { ptr -> String(nativeTox.toxGetStatusMessage(ptr)) }
    fun setStatusMessage(statusMessage: String) = withTox { ptr ->
        nativeTox.toxSetStatusMessage(ptr, statusMessage.toByteArray())
    }

    fun getToxId() = withTox { ptr -> ToxID.fromBytes(nativeTox.toxGetAddress(ptr)) }
    fun getPublicKey() = withTox { ptr -> PublicKey.fromBytes(nativeTox.toxGetPublicKey(ptr)) }

    fun getNospam(): Int = withTox { ptr -> nativeTox.toxGetNospam(ptr) }
    fun setNospam(value: Int) = withTox { ptr -> nativeTox.toxSetNospam(ptr, value) }

    fun getSaveData() = withTox { ptr -> nativeTox.toxGetSavedata(ptr) }

    // Adds a new contact by their full Tox ID and sends a friend request.
    fun addContact(toxId: ToxID, message: String) = withTox { ptr ->
        nativeTox.toxAddFriend(ptr, toxId.bytes(), message.toByteArray())
        updateContactMapping()
    }

    // Deletes a contact from the contact list by their public key.
    fun deleteContact(pk: PublicKey) = withTox { ptr ->
        Log.i(TAG, "Deleting ${pk.fingerprint()}")
        val friendNumber = nativeTox.toxFriendByPublicKey(ptr, pk.bytes())
        if (friendNumber != -1) {
            nativeTox.toxDeleteFriend(ptr, friendNumber)
        } else {
            Log.e(TAG, "Tried to delete nonexistent contact")
        }
        updateContactMapping()
    }

    // Returns a list of all friends as pairs of (PublicKey, native ID).
    fun getContacts(): List<Pair<PublicKey, Int>> = withTox { ptr ->
        val friendNumbers = nativeTox.toxGetFriendList(ptr)
        Log.i(TAG, "Loading ${friendNumbers.size} friends")
        List(friendNumbers.size) {
            Pair(PublicKey.fromBytes(nativeTox.toxGetFriendPublicKey(ptr, friendNumbers[it])), friendNumbers[it])
        }
    }

    fun getFriendPublicKey(friendNumber: Int): ByteArray? = withTox { ptr ->
        try {
            nativeTox.toxGetFriendPublicKey(ptr, friendNumber)
        } catch (_: Exception) {
            null
        }
    }

    // Sends a private text or action message to a friend.
    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int = withTox { ptr ->
        nativeTox.toxFriendSendMessage(
            ptr,
            contactByKey(ptr, publicKey),
            type.toToxType().ordinal,
            message.toByteArray(),
        )
    }

    // Accepts an incoming friend request and adds the contact to the list.
    fun acceptFriendRequest(pk: PublicKey): Result<Unit> = withTox { ptr ->
        try {
            nativeTox.toxAddFriendNorequest(ptr, pk.bytes())
            updateContactMapping()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while accepting friend request $pk: $e")
            Result.failure(e)
        }
    }

    // Adds a friend to the list without sending a request.
    fun addFriendNoRequest(pk: PublicKey): Int = withTox { ptr ->
        try {
            val result = nativeTox.toxAddFriendNorequest(ptr, pk.bytes())
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
    fun setTyping(publicKey: PublicKey, typing: Boolean) = withTox { ptr ->
        nativeTox.toxSetTyping(ptr, contactByKey(ptr, publicKey), typing)
    }

    fun getStatus() = withTox { ptr -> UserStatus.entries[nativeTox.toxGetSelfUserStatus(ptr)] }
    fun setStatus(status: UserStatus) = withTox { ptr ->
        nativeTox.toxSetSelfUserStatus(ptr, status.toToxType().ordinal)
    }

    // Sends a reliable lossless custom data packet to a friend.
    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): CustomPacketError = withTox { ptr ->
        try {
            nativeTox.toxFriendSendLosslessPacket(ptr, contactByKey(ptr, pk), packet)
            CustomPacketError.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending lossless packet: $e")
            CustomPacketError.Invalid
        }
    }

    // Sends an unreliable lossy custom data packet to a friend.
    fun sendLossyPacket(pk: PublicKey, data: ByteArray): CustomPacketError = withTox { ptr ->
        try {
            nativeTox.toxFriendSendLossyPacket(ptr, contactByKey(ptr, pk), data)
            CustomPacketError.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending lossy packet: $e")
            CustomPacketError.Invalid
        }
    }

    fun selfGetSecretKey(): ByteArray = withTox { ptr -> nativeTox.toxSelfGetSecretKey(ptr) }
    fun selfGetUdpPort(): Int = withTox { ptr -> nativeTox.toxSelfGetUdpPort(ptr) }
    fun selfGetTcpPort(): Int = withTox { ptr -> nativeTox.toxSelfGetTcpPort(ptr) }
    fun selfGetDhtId(): ByteArray = withTox { ptr -> nativeTox.toxSelfGetDhtId(ptr) }

    fun friendGetLastOnline(pk: PublicKey): Long = withTox { ptr ->
        nativeTox.toxFriendGetLastOnline(ptr, contactByKey(ptr, pk))
    }

    fun friendGetTyping(pk: PublicKey): Boolean = withTox { ptr ->
        nativeTox.toxFriendGetTyping(ptr, contactByKey(ptr, pk))
    }

    private fun contactByKey(ptr: Long, pk: PublicKey): Int {
        return nativeTox.toxFriendByPublicKey(ptr, pk.bytes())
    }

    fun getFriendNumberByPublicKey(pk: PublicKey): Int = withTox { ptr ->
        nativeTox.toxFriendByPublicKey(ptr, pk.bytes())
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
