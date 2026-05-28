// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.runtime.ToxRuntime
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.ToxID

@Singleton
class ToxImpl @Inject constructor(
    private val runtime: ToxRuntime,
) : ITox {
    override val toxId: ToxID get() = runtime.toxId
    override val publicKey: PublicKey get() = runtime.publicKey
    override var nospam: Int
        get() = runtime.nospam
        set(value) {
            runtime.nospam = value
        }

    override var started: Boolean
        get() = runtime.started
        set(_) = Unit

    override var isBootstrapNeeded: Boolean
        get() = runtime.isBootstrapNeeded
        set(value) {
            runtime.isBootstrapNeeded = value
        }

    override val password: String?
        get() = runtime.password

    fun start(saveOption: SaveOptions, password: String?, listener: ToxEventListener, avListener: ToxAvEventListener) {
        runtime.start(saveOption, password, listener, avListener)
    }

    override fun changePassword(new: String?) = runtime.changePassword(new)

    override fun stop() {
        runtime.stop()
    }

    override fun getContacts(): List<Pair<PublicKey, Int>> = runtime.getContacts()

    override fun acceptFriendRequest(publicKey: PublicKey) {
        runtime.acceptFriendRequest(publicKey)
    }

    override fun startFileTransfer(pk: PublicKey, fileNumber: Int) {
        runtime.startFileTransfer(pk, fileNumber)
    }

    override fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {
        runtime.stopFileTransfer(pk, fileNumber)
    }

    override fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int =
        runtime.sendFile(pk, fileKind, fileSize, fileName)

    override fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> =
        runtime.sendFileChunk(pk, fileNo, pos, data)

    override fun getName(): String = runtime.getName()
    override fun setName(name: String) {
        runtime.setName(name)
    }

    override fun getStatusMessage(): String = runtime.getStatusMessage()
    override fun setStatusMessage(statusMessage: String) {
        runtime.setStatusMessage(statusMessage)
    }

    override fun addContact(toxId: ToxID, message: String) {
        runtime.addContact(toxId, message)
    }

    override fun deleteContact(publicKey: PublicKey) {
        runtime.deleteContact(publicKey)
    }

    override fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int =
        runtime.sendMessage(publicKey, message, type)

    override fun getSaveData(): ByteArray = runtime.getSaveData()

    override fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean {
        runtime.setTyping(publicKey, typing)
        return true
    }

    override fun friendGetTyping(publicKey: PublicKey): Boolean = runtime.friendGetTyping(publicKey)

    override fun getFriendNumber(publicKey: PublicKey): Int = runtime.getFriendNumber(publicKey)

    override fun getFriendPublicKey(friendNumber: Int): PublicKey? {
        val bytes = runtime.getFriendPublicKey(friendNumber) ?: return null
        return PublicKey.fromBytes(bytes)
    }

    override fun friendGetLastOnline(publicKey: PublicKey): Long = runtime.friendGetLastOnline(publicKey)

    override fun getStatus(): UserStatus = runtime.getStatus()
    override fun setStatus(status: UserStatus) {
        runtime.setStatus(status)
    }

    override fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean {
        runtime.sendLosslessPacket(pk, packet)
        return true
    }

    override fun startCall(pk: PublicKey): Boolean = runtime.startCall(pk)
    override fun answerCall(pk: PublicKey): Boolean = runtime.answerCall(pk)
    override fun endCall(pk: PublicKey): Boolean = runtime.endCall(pk)
    override fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int): Boolean =
        runtime.sendAudio(pk, pcm, channels, samplingRate)

    override fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int =
        runtime.groupNew(privacyState, groupName, selfName)

    override fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int =
        runtime.groupJoin(friendNo, inviteData, selfName, password)

    override fun groupLeave(groupNumber: Int): Boolean =
        runtime.groupLeave(groupNumber)

    override fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int =
        runtime.groupSendMessage(groupNumber, type, message)

    override fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean =
        runtime.groupSetTopic(groupNumber, topic)

    override fun groupGetTopic(groupNumber: Int): ByteArray? =
        runtime.groupGetTopic(groupNumber)

    override fun groupGetName(groupNumber: Int): ByteArray? =
        runtime.groupGetName(groupNumber)

    override fun groupGetChatId(groupNumber: Int): ByteArray? =
        runtime.groupGetChatId(groupNumber)

    override fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean =
        runtime.groupSetPassword(groupNumber, password)

    override fun groupGetPassword(groupNumber: Int): ByteArray? =
        runtime.groupGetPassword(groupNumber)

    override fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? =
        runtime.groupPeerGetName(groupNumber, peerId)

    override fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? =
        runtime.groupPeerGetPublicKey(groupNumber, peerId)

    override fun groupSelfGetPeerId(groupNumber: Int): Int =
        runtime.groupSelfGetPeerId(groupNumber)

    override fun groupSelfGetRole(groupNumber: Int): ToxGroupRole =
        runtime.groupSelfGetRole(groupNumber)

    override fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean =
        runtime.groupInviteSend(groupNumber, friendNumber)

    override fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int =
        runtime.groupJoinDirect(chatId, selfName, password)

    override fun groupReconnect(groupNumber: Int): Boolean =
        runtime.groupReconnect(groupNumber)
}
