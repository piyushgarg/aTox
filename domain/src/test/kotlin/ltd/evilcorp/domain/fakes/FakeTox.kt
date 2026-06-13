package ltd.evilcorp.domain.fakes

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxMessageType

open class FakeTox(
    override val toxId: ToxID = ToxID("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C4ACEE797596D"),
    override val publicKey: PublicKey = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C"),
    override var nospam: Int = 123
) : FakeToxProfile(toxId, publicKey, nospam), ITox {
    override var started: Boolean = true
    override var isBootstrapNeeded: Boolean = false
    override val password: String? = null
    override val sessionId: String? = "dummy_session"

    val sentMessages = mutableListOf<Triple<PublicKey, String, MessageType>>()
    val sentTypingStatus = mutableMapOf<PublicKey, Boolean>()
    
    override fun changePassword(new: String?) {}
    override fun stop() { started = false }
    override fun getSaveData(): ByteArray = byteArrayOf(1, 2, 3)

    // IToxMessenger
    override fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int {
        sentMessages.add(Triple(publicKey, message, type))
        return sentMessages.size
    }
    override fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean {
        sentTypingStatus[publicKey] = typing
        return true
    }
    override fun friendGetTyping(publicKey: PublicKey): Boolean = sentTypingStatus[publicKey] ?: false
    override fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean = true

    // IToxFileTransmitter
    override fun startFileTransfer(pk: PublicKey, fileNumber: Int) {}
    override fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {}
    override fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int = 0
    override fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = Result.success(Unit)

    // IToxCallController
    override fun startCall(pk: PublicKey): Boolean = true
    override fun answerCall(pk: PublicKey): Boolean = true
    override fun endCall(pk: PublicKey): Boolean = true
    override fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int): Boolean = true

    // IToxGroupManager
    override fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int = 0
    override fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int = 0
    override fun groupLeave(groupNumber: Int): Boolean = true
    override fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int = 0
    override fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = true
    override fun groupGetTopic(groupNumber: Int): ByteArray? = null
    override fun groupGetName(groupNumber: Int): ByteArray? = null
    override fun groupGetChatId(groupNumber: Int): ByteArray? = null
    override fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = true
    override fun groupGetPassword(groupNumber: Int): ByteArray? = null
    override fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = null
    override fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = null
    override fun groupSelfGetPeerId(groupNumber: Int): Int = 0
    override fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = ToxGroupRole.USER
    override fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean = true
    override fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int = 0
    override fun groupReconnect(groupNumber: Int): Boolean = true
    override fun groupGetChatlist(): IntArray = intArrayOf()
}
