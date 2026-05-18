package ltd.evilcorp.domain.tox

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.tox.save.SaveOptions
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.ToxID
import ltd.evilcorp.core.tox.runtime.ToxRuntime
import ltd.evilcorp.core.model.FileKind
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.UserStatus

@Singleton
class Tox @Inject constructor(
    private val runtime: ToxRuntime,
) {
    val toxId: ToxID get() = runtime.toxId
    val publicKey: PublicKey get() = runtime.publicKey
    var nospam: Int
        get() = runtime.nospam
        set(value) {
            runtime.nospam = value
        }

    var started: Boolean
        get() = runtime.started
        private set(_) = Unit

    var isBootstrapNeeded: Boolean
        get() = runtime.isBootstrapNeeded
        set(value) {
            runtime.isBootstrapNeeded = value
        }

    val password: String?
        get() = runtime.password

    fun changePassword(new: String?) {
        runtime.changePassword(new)
    }

    fun start(saveOption: SaveOptions, password: String?, listener: ToxEventListener, avListener: ToxAvEventListener) {
        runtime.start(saveOption, password, listener, avListener)
    }

    fun stop() = runtime.stop()

    fun getContacts(): List<Pair<PublicKey, Int>> = runtime.getContacts()

    fun acceptFriendRequest(publicKey: PublicKey) {
        runtime.acceptFriendRequest(publicKey)
    }

    fun startFileTransfer(pk: PublicKey, fileNumber: Int) = runtime.startFileTransfer(pk, fileNumber)

    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) = runtime.stopFileTransfer(pk, fileNumber)

    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String) =
        runtime.sendFile(pk, fileKind, fileSize, fileName)

    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> =
        runtime.sendFileChunk(pk, fileNo, pos, data)

    fun getName() = runtime.getName()
    fun setName(name: String) {
        runtime.setName(name)
    }

    fun getStatusMessage() = runtime.getStatusMessage()
    fun setStatusMessage(statusMessage: String) {
        runtime.setStatusMessage(statusMessage)
    }

    fun addContact(toxId: ToxID, message: String) {
        runtime.addContact(toxId, message)
    }

    fun deleteContact(publicKey: PublicKey) {
        runtime.deleteContact(publicKey)
    }

    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType) =
        runtime.sendMessage(publicKey, message, type)

    fun getSaveData(): ByteArray = runtime.getSaveData()

    fun setTyping(publicKey: PublicKey, typing: Boolean) = runtime.setTyping(publicKey, typing)

    fun getStatus() = runtime.getStatus()
    fun setStatus(status: UserStatus) {
        runtime.setStatus(status)
    }

    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray) = runtime.sendLosslessPacket(pk, packet)

    fun startCall(pk: PublicKey) = runtime.startCall(pk)
    fun answerCall(pk: PublicKey) = runtime.answerCall(pk)
    fun endCall(pk: PublicKey) = runtime.endCall(pk)
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) =
        runtime.sendAudio(pk, pcm, channels, samplingRate)
}
