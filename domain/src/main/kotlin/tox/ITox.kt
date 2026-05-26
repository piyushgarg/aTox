// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox

import ltd.evilcorp.core.tox.ToxID
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.FileKind
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.core.tox.enums.ToxGroupRole
import ltd.evilcorp.core.tox.enums.ToxMessageType

interface ITox {
    val toxId: ToxID
    val publicKey: PublicKey
    var nospam: Int
    var started: Boolean
    var isBootstrapNeeded: Boolean
    val password: String?

    fun changePassword(new: String?)
    fun stop()
    fun getContacts(): List<Pair<PublicKey, Int>>
    fun acceptFriendRequest(publicKey: PublicKey)
    fun startFileTransfer(pk: PublicKey, fileNumber: Int)
    fun stopFileTransfer(pk: PublicKey, fileNumber: Int)
    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int
    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit>
    fun getName(): String
    fun setName(name: String)
    fun getStatusMessage(): String
    fun setStatusMessage(statusMessage: String)
    fun addContact(toxId: ToxID, message: String)
    fun deleteContact(publicKey: PublicKey)
    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int
    fun getSaveData(): ByteArray
    fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean
    fun friendGetTyping(publicKey: PublicKey): Boolean
    fun getFriendNumber(publicKey: PublicKey): Int
    fun getFriendPublicKey(friendNumber: Int): PublicKey?
    fun friendGetLastOnline(publicKey: PublicKey): Long
    fun getStatus(): UserStatus
    fun setStatus(status: UserStatus)
    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean
    fun startCall(pk: PublicKey): Boolean
    fun answerCall(pk: PublicKey): Boolean
    fun endCall(pk: PublicKey): Boolean
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int): Boolean
    fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int
    fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int
    fun groupLeave(groupNumber: Int): Boolean
    fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int
    fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean
    fun groupGetTopic(groupNumber: Int): ByteArray?
    fun groupGetName(groupNumber: Int): ByteArray?
    fun groupGetChatId(groupNumber: Int): ByteArray?
    fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean
    fun groupGetPassword(groupNumber: Int): ByteArray?
    fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray?
    fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray?
    fun groupSelfGetPeerId(groupNumber: Int): Int
    fun groupSelfGetRole(groupNumber: Int): ToxGroupRole
    fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean
    fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int
    fun groupReconnect(groupNumber: Int): Boolean
}
