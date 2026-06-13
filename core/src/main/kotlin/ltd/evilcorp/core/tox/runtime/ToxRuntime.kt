package ltd.evilcorp.core.tox.runtime

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import ltd.evilcorp.domain.core.di.IoDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.save.SaveOptions

private const val TAG = "ToxRuntime"
private const val TOX_SALT_LENGTH = 32
private const val STOP_DELAY_MS = 10L

/**
 * Clean architectural facade for Tox JNI operations.
 * Decomposes running, saving/crypto, and media bridge concerns to dedicated helper classes.
 */
@Suppress("unused")
@Singleton
class ToxRuntime @Inject constructor(
    private val scope: CoroutineScope,
    private val sessionSaver: ToxSessionSaver,
    private val engine: ToxEngine,
    private val callBridge: ToxCallBridge,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    val toxId: ToxID get() = toxWrapper.getToxId()
    val publicKey: PublicKey get() = toxWrapper.getPublicKey()

    var nospam: Int
        get() = toxWrapper.getNospam()
        set(value) = toxWrapper.setNospam(value)

    var started = false
        private set

    var isBootstrapNeeded: Boolean
        get() = engine.isBootstrapNeeded
        set(value) { engine.isBootstrapNeeded = value }

    var password: String? = null
        private set

    var sessionId: String? = null
        private set

    private var passkey: ByteArray? = null
    private lateinit var toxWrapper: ToxWrapper

    private val saveMutex = Mutex()

    fun start(saveOption: SaveOptions, password: String?, listener: ToxEventListener, avListener: ToxAvEventListener) {
        sessionId = java.util.UUID.randomUUID().toString()
        val nativeTox = NativeTox()
        toxWrapper = if (password == null) {
            passkey = null
            ToxWrapper(listener, avListener, saveOption)
        } else {
            val salt = nativeTox.getSalt(saveOption.saveData ?: ByteArray(0))
            if (salt != null) {
                passkey = sessionSaver.derivePasskey(password, salt)
            }
            ToxWrapper(
                listener,
                avListener,
                saveOption.copy(
                    saveData = if (passkey != null) {
                        nativeTox.passDecrypt(saveOption.saveData ?: ByteArray(0), passkey!!)
                    } else {
                        saveOption.saveData
                    },
                ),
            )
        }

        callBridge.init(toxWrapper)
        this.password = password
        started = true
        save()
        engine.start(toxWrapper) {
            started = false
        }
    }

    private var stopJob: Job? = null

    fun stop(): Job {
        val job = scope.launch {
            engine.stop()
            while (started) {
                delay(STOP_DELAY_MS.milliseconds)
            }
            save().join()
            toxWrapper.stop()
            passkey = null
        }
        stopJob = job
        return job
    }

    fun waitForStop() {
        runBlocking {
            stopJob?.join()
        }
    }

    fun changePassword(new: String?) {
        passkey = if (new.isNullOrEmpty()) {
            null
        } else {
            val salt = ByteArray(TOX_SALT_LENGTH)
            Random.nextBytes(salt)
            sessionSaver.derivePasskey(new, salt)
        }
        password = new
        save()
    }

    fun getContacts(): List<Pair<PublicKey, Int>> = toxWrapper.getContacts()

    fun acceptFriendRequest(publicKey: PublicKey): Result<Unit> {
        val result = toxWrapper.acceptFriendRequest(publicKey)
        if (result.isSuccess) {
            save()
        }
        return result
    }

    fun addFriendNoRequest(publicKey: PublicKey): Int {
        val result = toxWrapper.addFriendNoRequest(publicKey)
        if (result >= 0) save()
        return result
    }

    fun startFileTransfer(pk: PublicKey, fileNumber: Int) {
        Log.i(TAG, "Starting file transfer $fileNumber from ${pk.fingerprint()}")
        toxWrapper.startFileTransfer(pk, fileNumber)
    }

    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {
        Log.i(TAG, "Stopping file transfer $fileNumber from ${pk.fingerprint()}")
        toxWrapper.stopFileTransfer(pk, fileNumber)
    }

    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String) =
        toxWrapper.sendFile(pk, fileKind, fileSize, fileName)

    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> =
        toxWrapper.sendFileChunk(pk, fileNo, pos, data)

    fun getName() = toxWrapper.getName()

    fun setName(name: String) {
        toxWrapper.setName(name)
        save()
    }

    fun getStatusMessage() = toxWrapper.getStatusMessage()

    fun setStatusMessage(statusMessage: String) {
        toxWrapper.setStatusMessage(statusMessage)
        save()
    }

    fun addContact(toxId: ToxID, message: String) {
        toxWrapper.addContact(toxId, message)
        save()
    }


    fun deleteContact(publicKey: PublicKey) {
        toxWrapper.deleteContact(publicKey)
        save()
    }

    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType) =
        toxWrapper.sendMessage(publicKey, message, type)

    fun getSaveData(): ByteArray {
        val currentPasskey = passkey
        val saveData = toxWrapper.getSaveData()
        return if (currentPasskey == null) {
            saveData
        } else {
            NativeTox().passEncrypt(saveData, currentPasskey) ?: saveData
        }
    }

    fun setTyping(publicKey: PublicKey, typing: Boolean) =
        toxWrapper.setTyping(publicKey, typing)

    fun getStatus() = toxWrapper.getStatus()

    fun setStatus(status: UserStatus) {
        toxWrapper.setStatus(status)
        save()
    }

    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray) =
        toxWrapper.sendLosslessPacket(pk, packet)

    fun sendLossyPacket(pk: PublicKey, data: ByteArray) =
        toxWrapper.sendLossyPacket(pk, data)

    fun selfGetSecretKey(): ByteArray =
        toxWrapper.selfGetSecretKey()

    fun selfGetUdpPort(): Int =
        toxWrapper.selfGetUdpPort()

    fun selfGetTcpPort(): Int =
        toxWrapper.selfGetTcpPort()

    fun selfGetDhtId(): ByteArray =
        toxWrapper.selfGetDhtId()

    fun friendGetLastOnline(pk: PublicKey): Long =
        toxWrapper.friendGetLastOnline(pk)

    fun friendGetTyping(pk: PublicKey): Boolean =
        toxWrapper.friendGetTyping(pk)

    fun getFriendNumber(pk: PublicKey): Int =
        toxWrapper.getFriendNumberByPublicKey(pk)

    fun getFriendPublicKey(friendNumber: Int): ByteArray? =
        toxWrapper.getFriendPublicKey(friendNumber)

    fun startCall(pk: PublicKey) = callBridge.startCall(pk)
    fun answerCall(pk: PublicKey) = callBridge.answerCall(pk)
    fun endCall(pk: PublicKey) = callBridge.endCall(pk)

    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) =
        callBridge.sendAudio(pk, pcm, channels, samplingRate)

    fun sendVideoFrame(pk: PublicKey, width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray): Boolean =
        callBridge.sendVideoFrame(pk, width, height, y, u, v)

    fun audioSetBitRate(pk: PublicKey, bitrate: Int): Boolean =
        callBridge.audioSetBitRate(pk, bitrate)

    fun videoSetBitRate(pk: PublicKey, bitrate: Int): Boolean =
        callBridge.videoSetBitRate(pk, bitrate)

    fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int {
        val result = toxWrapper.groupNew(privacyState, groupName, selfName)
        if (result >= 0) save()
        return result
    }

    fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int {
        val result = toxWrapper.groupJoin(friendNo, inviteData, selfName, password)
        if (result >= 0) save()
        return result
    }

    fun groupLeave(groupNumber: Int): Boolean {
        val result = toxWrapper.groupLeave(groupNumber)
        if (result) save()
        return result
    }

    fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int =
        toxWrapper.groupSendMessage(groupNumber, type, message)

    fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean =
        toxWrapper.groupSetTopic(groupNumber, topic)

    fun groupGetTopic(groupNumber: Int): ByteArray? =
        toxWrapper.groupGetTopic(groupNumber)

    fun groupGetName(groupNumber: Int): ByteArray? =
        toxWrapper.groupGetName(groupNumber)

    fun groupGetChatId(groupNumber: Int): ByteArray? =
        toxWrapper.groupGetChatId(groupNumber)

    fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean {
        val result = toxWrapper.groupSetPassword(groupNumber, password)
        if (result) save()
        return result
    }

    fun groupGetPassword(groupNumber: Int): ByteArray? =
        toxWrapper.groupGetPassword(groupNumber)

    fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? =
        toxWrapper.groupPeerGetName(groupNumber, peerId)

    fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? =
        toxWrapper.groupPeerGetPublicKey(groupNumber, peerId)

    fun groupSelfGetPeerId(groupNumber: Int): Int =
        toxWrapper.groupSelfGetPeerId(groupNumber)

    fun groupSelfGetRole(groupNumber: Int): ToxGroupRole =
        toxWrapper.groupSelfGetRole(groupNumber)

    fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean =
        toxWrapper.groupInviteSend(groupNumber, friendNumber)

    fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int {
        val result = toxWrapper.groupJoinDirect(chatId, selfName, password)
        if (result >= 0) save()
        return result
    }

    fun groupReconnect(groupNumber: Int): Boolean =
        toxWrapper.groupReconnect(groupNumber)

    fun groupGetChatlist(): IntArray =
        toxWrapper.groupGetChatlist()
    fun groupavAdd(): Int =
        toxWrapper.groupavAdd()

    fun groupavJoin(groupNumber: Int): Int =
        toxWrapper.groupavJoin(groupNumber)

    fun groupavSendAudio(groupNumber: Int, pcm: ShortArray, channels: Int, samplingRate: Int): Int =
        toxWrapper.groupavSendAudio(groupNumber, pcm, channels, samplingRate)

    fun groupavEnableAudio(groupNumber: Int): Int =
        toxWrapper.groupavEnableAudio(groupNumber)

    fun groupavDisableAudio(groupNumber: Int): Int =
        toxWrapper.groupavDisableAudio(groupNumber)

    fun groupavIsEnabled(groupNumber: Int): Boolean =
        toxWrapper.groupavIsEnabled(groupNumber)

    fun save(): Job = scope.launch {
        saveMutex.withLock {
            if (!started) return@withLock
            withContext(ioDispatcher) {
                sessionSaver.encryptAndSave(publicKey, toxWrapper.getSaveData(), passkey)
            }
        }
    }
}
