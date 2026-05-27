package ltd.evilcorp.core.tox.runtime

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.forEach as kForEach
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ltd.evilcorp.domain.model.FileKind
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.domain.tox.ToxID
import ltd.evilcorp.domain.tox.bootstrap.BootstrapNodeRegistry
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.domain.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.tox.enums.ToxGroupRole
import ltd.evilcorp.domain.tox.enums.ToxMessageType
import ltd.evilcorp.core.tox.save.SaveManager
import ltd.evilcorp.domain.tox.save.SaveOptions

private const val TAG = "ToxRuntime"
private const val SLOW_ITERATION_LIMIT_MS = 10
private const val TOX_SALT_LENGTH = 32
private const val STOP_DELAY_MS = 10L
private const val BOOTSTRAP_NODES_COUNT = 4
private const val RECOVERY_DELAY_MS = 1000L

/**
 * Main execution layer (Tox Runtime).
 * Manages coroutine loops for background event processing (`iterate()`), periodic
 * session autosaving, and proxies Tox core function calls from the application layer.
 */
@Singleton
class ToxRuntime @Inject constructor(
    private val scope: CoroutineScope,
    private val saveManager: SaveManager,
    private val nodeRegistry: BootstrapNodeRegistry,
) {
    /** Unique Tox ID of the current session (76 characters). */
    val toxId: ToxID get() = toxWrapper.getToxId()

    /** Public key of the current user (32 bytes). */
    val publicKey: PublicKey get() = toxWrapper.getPublicKey()

    /**
     * System nospam value of the user.
     * Used to protect against unwanted friend requests.
     */
    var nospam: Int
        get() = toxWrapper.getNospam()
        set(value) = toxWrapper.setNospam(value)

    /** Indicates if the Tox session is currently running. */
    var started = false
        private set

    /** Indicates if reconnection to public DHT nodes (bootstrap) is required. */
    var isBootstrapNeeded = true

    /** Current profile encryption password (null if profile is not encrypted). */
    var password: String? = null
        private set

    private var running = false
    private var toxAvRunning = false
    private var passkey: ByteArray? = null
    private lateinit var toxWrapper: ToxWrapper

    private val saveMutex = Mutex()

    /**
     * Starts the native Tox core, decrypts the profile if a password is provided, and starts async iteration loops.
     * @param saveOption Initialization parameters and binary profile data.
     * @param password Profile encryption password.
     * @param listener Listener for basic Tox events (messages, statuses, files).
     * @param avListener Listener for audio and video calls.
     */
    fun start(saveOption: SaveOptions, password: String?, listener: ToxEventListener, avListener: ToxAvEventListener) {
        val nativeTox = NativeTox()
        toxWrapper = if (password == null) {
            passkey = null
            ToxWrapper(listener, avListener, saveOption)
        } else {
            val salt = nativeTox.getSalt(saveOption.saveData ?: ByteArray(0))
            if (salt != null) {
                passkey = nativeTox.passKeyDeriveWithSalt(password.toByteArray(), salt)
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

        this.password = password
        started = true
        save()
        iterateForever()
        iterateForeverAv()
    }

    /**
     * Stops the Tox session, terminates processing loops, saves the profile to disk, and clears encryption keys in memory.
     * @return Coroutine [Job] object.
     */
    fun stop(): Job = scope.launch {
        running = false
        while (started) {
            delay(STOP_DELAY_MS)
        }
        save().join()
        toxWrapper.stop()
        passkey = null
    }

    /**
     * Changes or removes the encryption password of the current profile, then initiates an immediate save.
     * @param new New password. Pass null or empty string to remove encryption.
     */
    fun changePassword(new: String?) {
        passkey = if (new.isNullOrEmpty()) {
            null
        } else {
            val salt = ByteArray(TOX_SALT_LENGTH)
            Random.Default.nextBytes(salt)
            NativeTox().passKeyDeriveWithSalt(new.toByteArray(), salt)
        }
        password = new
        save()
    }

    /**
     * Returns a list of all friends added to the Tox core contact list.
     */
    fun getContacts(): List<Pair<PublicKey, Int>> = toxWrapper.getContacts()

    /**
     * Accepts a friend request from another user.
     * @param publicKey Public key of the request sender.
     */
    fun acceptFriendRequest(publicKey: PublicKey) {
        toxWrapper.acceptFriendRequest(publicKey)
        save()
    }

    /**
     * Initiates resumption or acceptance of a file transfer.
     */
    fun startFileTransfer(pk: PublicKey, fileNumber: Int) {
        Log.i(TAG, "Starting file transfer $fileNumber from ${pk.fingerprint()}")
        toxWrapper.startFileTransfer(pk, fileNumber)
    }

    /**
     * Pauses a file transfer.
     */
    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {
        Log.i(TAG, "Stopping file transfer $fileNumber from ${pk.fingerprint()}")
        toxWrapper.stopFileTransfer(pk, fileNumber)
    }

    /**
     * Sends a file transfer request to a friend.
     */
    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String) =
        toxWrapper.sendFile(pk, fileKind, fileSize, fileName)

    /**
     * Sends another binary chunk of data in the file transfer process.
     */
    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> =
        toxWrapper.sendFileChunk(pk, fileNo, pos, data)

    /**
     * Returns the public name of the current user.
     */
    fun getName() = toxWrapper.getName()

    /**
     * Sets a new username and saves the profile.
     */
    fun setName(name: String) {
        toxWrapper.setName(name)
        save()
    }

    /**
     * Returns the current status message of the user.
     */
    fun getStatusMessage() = toxWrapper.getStatusMessage()

    /**
     * Sets a new status message and saves the profile.
     */
    fun setStatusMessage(statusMessage: String) {
        toxWrapper.setStatusMessage(statusMessage)
        save()
    }

    /**
     * Sends a request to add a new friend by their Tox ID address.
     */
    fun addContact(toxId: ToxID, message: String) {
        toxWrapper.addContact(toxId, message)
        save()
    }

    /**
     * Deletes a friend from the contact list.
     */
    fun deleteContact(publicKey: PublicKey) {
        toxWrapper.deleteContact(publicKey)
        save()
    }

    /**
     * Sends a text message to a friend.
     */
    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType) =
        toxWrapper.sendMessage(publicKey, message, type)

    /**
     * Returns a full copy of the binary profile data (encrypted if password is set).
     */
    fun getSaveData(): ByteArray {
        val currentPasskey = passkey
        val saveData = toxWrapper.getSaveData()
        return if (currentPasskey == null) {
            saveData
        } else {
            NativeTox().passEncrypt(saveData, currentPasskey) ?: saveData
        }
    }

    /**
     * Sets or clears the typing indicator for a friend.
     */
    fun setTyping(publicKey: PublicKey, typing: Boolean) =
        toxWrapper.setTyping(publicKey, typing)

    /**
     * Returns the current network presence status (Online, Away, Busy).
     */
    fun getStatus() = toxWrapper.getStatus()

    /**
     * Sets a new presence status for the user and saves the profile.
     */
    fun setStatus(status: UserStatus) {
        toxWrapper.setStatus(status)
        save()
    }

    /**
     * Sends an arbitrary lossless custom packet to a friend.
     */
    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray) =
        toxWrapper.sendLosslessPacket(pk, packet)

    /**
     * Sends a custom lossy packet to a friend.
     */
    fun sendLossyPacket(pk: PublicKey, data: ByteArray) =
        toxWrapper.sendLossyPacket(pk, data)

    /**
     * Returns the secret (private) key of our profile.
     */
    fun selfGetSecretKey(): ByteArray =
        toxWrapper.selfGetSecretKey()

    /**
     * Returns the active UDP port of the local node.
     */
    fun selfGetUdpPort(): Int =
        toxWrapper.selfGetUdpPort()

    /**
     * Returns the active TCP port of the local node.
     */
    fun selfGetTcpPort(): Int =
        toxWrapper.selfGetTcpPort()

    /**
     * Returns the temporary DHT key (DHT ID) of our instance.
     */
    fun selfGetDhtId(): ByteArray =
        toxWrapper.selfGetDhtId()

    /**
     * Returns the UNIX time of the contact's last network visit.
     */
    fun friendGetLastOnline(pk: PublicKey): Long =
        toxWrapper.friendGetLastOnline(pk)

    /**
     * Returns the active typing status of a friend.
     */
    fun friendGetTyping(pk: PublicKey): Boolean =
        toxWrapper.friendGetTyping(pk)

    /**
     * Returns the native friend number by public key.
     */
    fun getFriendNumber(pk: PublicKey): Int =
        toxWrapper.getFriendNumberByPublicKey(pk)

    /**
     * Returns the friend's public key by their native number.
     */
    fun getFriendPublicKey(friendNumber: Int): ByteArray? =
        toxWrapper.getFriendPublicKey(friendNumber)

    /** Starts an audio/video call. */
    fun startCall(pk: PublicKey) = toxWrapper.startCall(pk)
    /** Answers an incoming call. */
    fun answerCall(pk: PublicKey) = toxWrapper.answerCall(pk)
    /** Ends a call. */
    fun endCall(pk: PublicKey) = toxWrapper.endCall(pk)

    /** Sends a PCM audio frame to the call participant. */
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) =
        toxWrapper.sendAudio(pk, pcm, channels, samplingRate)

    /**
     * Sends a YUV420P video frame to the peer during a call.
     */
    fun sendVideoFrame(pk: PublicKey, width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray): Boolean =
        toxWrapper.sendVideoFrame(pk, width, height, y, u, v)

    /**
     * Dynamically adjusts the audio stream bitrate during a call.
     */
    fun audioSetBitRate(pk: PublicKey, bitrate: Int): Boolean =
        toxWrapper.audioSetBitRate(pk, bitrate)

    /**
     * Dynamically adjusts the video stream bitrate during a call.
     */
    fun videoSetBitRate(pk: PublicKey, bitrate: Int): Boolean =
        toxWrapper.videoSetBitRate(pk, bitrate)

    /**
     * Creates a new group NGC conference.
     */
    fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int =
        toxWrapper.groupNew(privacyState, groupName, selfName)

    /**
     * Joins a group NGC conference.
     */
    fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int =
        toxWrapper.groupJoin(friendNo, inviteData, selfName, password)

    /**
     * Leaves a group NGC conference.
     */
    fun groupLeave(groupNumber: Int): Boolean =
        toxWrapper.groupLeave(groupNumber)

    /**
     * Sends a text message to the NGC group.
     */
    fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int =
        toxWrapper.groupSendMessage(groupNumber, type, message)

    /**
     * Sets the topic for the NGC group.
     */
    fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean =
        toxWrapper.groupSetTopic(groupNumber, topic)

    /**
     * Gets the topic of the NGC group.
     */
    fun groupGetTopic(groupNumber: Int): ByteArray? =
        toxWrapper.groupGetTopic(groupNumber)

    /**
     * Gets the name of the NGC group.
     */
    fun groupGetName(groupNumber: Int): ByteArray? =
        toxWrapper.groupGetName(groupNumber)

    /**
     * Returns a unique persistent 32-byte NGC chat ID (Chat ID).
     */
    fun groupGetChatId(groupNumber: Int): ByteArray? =
        toxWrapper.groupGetChatId(groupNumber)

    /**
     * Sets the password for accessing the NGC group.
     */
    fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean =
        toxWrapper.groupSetPassword(groupNumber, password)

    /**
     * Returns the currently set password of the group.
     */
    fun groupGetPassword(groupNumber: Int): ByteArray? =
        toxWrapper.groupGetPassword(groupNumber)

    /**
     * Gets the name of the NGC group member by their ID.
     */
    fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? =
        toxWrapper.groupPeerGetName(groupNumber, peerId)

    /**
     * Gets the public key of the NGC group member by their ID.
     */
    fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? =
        toxWrapper.groupPeerGetPublicKey(groupNumber, peerId)

    /**
     * Returns our own Peer ID in the NGC group.
     */
    fun groupSelfGetPeerId(groupNumber: Int): Int =
        toxWrapper.groupSelfGetPeerId(groupNumber)

    /**
     * Returns our current role in the NGC group.
     */
    fun groupSelfGetRole(groupNumber: Int): ToxGroupRole =
        toxWrapper.groupSelfGetRole(groupNumber)

    /**
     * Sends a group invitation to a friend.
     */
    fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean =
        toxWrapper.groupInviteSend(groupNumber, friendNumber)

    /**
     * Joins an NGC group directly by Chat ID.
     */
    fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int =
        toxWrapper.groupJoinDirect(chatId, selfName, password)

    /**
     * Reconnects to a previously saved NGC group after profile load.
     */
    fun groupReconnect(groupNumber: Int): Boolean =
        toxWrapper.groupReconnect(groupNumber)

    /**
     * Creates a group audio conference.
     */
    fun groupavAdd(): Int =
        toxWrapper.groupavAdd()

    /**
     * Joins a group audio conference.
     */
    fun groupavJoin(groupNumber: Int): Int =
        toxWrapper.groupavJoin(groupNumber)

    /**
     * Sends an audio frame to the group chat.
     */
    fun groupavSendAudio(groupNumber: Int, pcm: ShortArray, channels: Int, samplingRate: Int): Int =
        toxWrapper.groupavSendAudio(groupNumber, pcm, channels, samplingRate)

    /**
     * Enables audio/video features for the specified group chat.
     */
    fun groupavEnableAudio(groupNumber: Int): Int =
        toxWrapper.groupavEnableAudio(groupNumber)

    /**
     * Disables audio/video features for the specified group chat.
     */
    fun groupavDisableAudio(groupNumber: Int): Int =
        toxWrapper.groupavDisableAudio(groupNumber)

    /**
     * Checks if audio/video features are active in the specified group chat.
     */
    fun groupavIsEnabled(groupNumber: Int): Boolean =
        toxWrapper.groupavIsEnabled(groupNumber)

    /** Background loop for processing audio and video calls. */
    private fun iterateForeverAv() = scope.launch {
        toxAvRunning = true
        while (running) {
            try {
                toxWrapper.iterateAv()
                delay(toxWrapper.iterationIntervalAv().coerceAtLeast(0L))
            } catch (e: Exception) {
                Log.e(TAG, "Error in ToxAv iteration loop: $e")
                delay(RECOVERY_DELAY_MS)
            }
        }
        toxAvRunning = false
    }

    /** Main background loop for processing Tox Core P2P network events. */
    private fun iterateForever() = scope.launch {
        running = true
        while (running || toxAvRunning) {
            try {
                if (isBootstrapNeeded) {
                    try {
                        bootstrap()
                        isBootstrapNeeded = false
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }

                val before = System.currentTimeMillis()
                toxWrapper.iterate()
                val timeTaken = System.currentTimeMillis() - before
                val iterationInterval = toxWrapper.iterationInterval()
                if (timeTaken > SLOW_ITERATION_LIMIT_MS && timeTaken > iterationInterval) {
                    Log.w(TAG, "Tox thread overran: $timeTaken/$iterationInterval.")
                }
                delay((iterationInterval - timeTaken).coerceAtLeast(0L))
            } catch (e: Exception) {
                Log.e(TAG, "Error in Tox iteration loop: $e")
                delay(RECOVERY_DELAY_MS)
            }
        }
        started = false
    }

    /** Connects to a predefined pool of public DHT servers (Bootstrap). */
    private fun bootstrap() {
        nodeRegistry.get(BOOTSTRAP_NODES_COUNT).kForEach { node ->
            Log.i(TAG, "Bootstrapping from $node")
            toxWrapper.bootstrap(node.address, node.port, node.publicKey.bytes())
        }
    }

    /** Background asynchronous saving of settings file to device storage. */
    private fun save(): Job = scope.launch {
        saveMutex.withLock {
            if (!started) {
                return@withLock
            }

            val saveData = toxWrapper.getSaveData()
            val encryptedData = if (passkey != null) {
                NativeTox().passEncrypt(saveData, passkey!!) ?: saveData
            } else {
                saveData
            }
            saveManager.save(publicKey, encryptedData)
        }
    }
}
