// SPDX-FileCopyrightText: 2019 aTox contributors
// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox

import ltd.evilcorp.domain.tox.enums.ToxConnection
import ltd.evilcorp.domain.tox.enums.ToxFileControl
import ltd.evilcorp.domain.tox.enums.ToxMessageType
import ltd.evilcorp.domain.tox.enums.ToxUserStatus
import javax.inject.Inject
import ltd.evilcorp.core.vo.ConnectionStatus
import ltd.evilcorp.core.vo.PublicKey
import ltd.evilcorp.core.vo.UserStatus

typealias FriendLosslessPacketHandler = (publicKey: String, data: ByteArray) -> Unit
typealias FileRecvControlHandler = (publicKey: String, fileNo: Int, control: ToxFileControl) -> Unit
typealias FriendStatusMessageHandler = (publicKey: String, message: String) -> Unit
typealias FriendReadReceiptHandler = (publicKey: String, messageId: Int) -> Unit
typealias FriendStatusHandler = (publicKey: String, status: UserStatus) -> Unit
typealias FriendConnectionStatusHandler = (publicKey: String, status: ConnectionStatus) -> Unit
typealias FriendRequestHandler = (publicKey: String, timeDelta: Int, message: String) -> Unit
typealias FriendMessageHandler = (
    publicKey: String,
    messageType: ToxMessageType,
    timeDelta: Int,
    message: String,
) -> Unit
typealias FriendNameHandler = (publicKey: String, newName: String) -> Unit
typealias FileRecvChunkHandler = (publicKey: String, fileNo: Int, position: Long, data: ByteArray) -> Unit
typealias FileRecvHandler = (publicKey: String, fileNo: Int, kind: Int, size: Long, name: String) -> Unit
typealias FriendLossyPacketHandler = (publicKey: String, data: ByteArray) -> Unit
typealias SelfConnectionStatusHandler = (status: ConnectionStatus) -> Unit
typealias FriendTypingHandler = (publicKey: String, isTyping: Boolean) -> Unit
typealias FileChunkRequestHandler = (publicKey: String, fileNo: Int, position: Long, length: Int) -> Unit

class ToxEventListener @Inject constructor() {
    var contactMapping: List<Pair<PublicKey, Int>> = listOf()

    var friendLosslessPacketHandler: FriendLosslessPacketHandler = { _, _ -> }
    var fileRecvControlHandler: FileRecvControlHandler = { _, _, _ -> }
    var friendStatusMessageHandler: FriendStatusMessageHandler = { _, _ -> }
    var friendReadReceiptHandler: FriendReadReceiptHandler = { _, _ -> }
    var friendStatusHandler: FriendStatusHandler = { _, _ -> }
    var friendConnectionStatusHandler: FriendConnectionStatusHandler = { _, _ -> }
    var friendRequestHandler: FriendRequestHandler = { _, _, _ -> }
    var friendMessageHandler: FriendMessageHandler = { _, _, _, _ -> }
    var friendNameHandler: FriendNameHandler = { _, _ -> }
    var fileRecvChunkHandler: FileRecvChunkHandler = { _, _, _, _ -> }
    var fileRecvHandler: FileRecvHandler = { _, _, _, _, _ -> }
    var friendLossyPacketHandler: FriendLossyPacketHandler = { _, _ -> }
    var selfConnectionStatusHandler: SelfConnectionStatusHandler = { _ -> }
    var friendTypingHandler: FriendTypingHandler = { _, _ -> }
    var fileChunkRequestHandler: FileChunkRequestHandler = { _, _, _, _ -> }

    private fun keyFor(friendNo: Int) = contactMapping.find { it.second == friendNo }!!.first.string()

    fun friendLosslessPacket(friendNo: Int, data: ByteArray) =
        friendLosslessPacketHandler(keyFor(friendNo), data)

    fun fileRecvControl(friendNo: Int, fileNo: Int, control: ToxFileControl) =
        fileRecvControlHandler(keyFor(friendNo), fileNo, control)

    fun friendStatusMessage(friendNo: Int, message: ByteArray) =
        friendStatusMessageHandler(keyFor(friendNo), String(message))

    fun friendReadReceipt(friendNo: Int, messageId: Int) =
        friendReadReceiptHandler(keyFor(friendNo), messageId)

    fun friendStatus(friendNo: Int, status: ToxUserStatus) =
        friendStatusHandler(keyFor(friendNo), status.toUserStatus())

    fun friendConnectionStatus(friendNo: Int, status: ToxConnection) =
        friendConnectionStatusHandler(keyFor(friendNo), status.toConnectionStatus())

    fun friendRequest(publicKey: ByteArray, timeDelta: Int, message: ByteArray) =
        friendRequestHandler(publicKey.bytesToHex(), timeDelta, String(message))

    fun friendMessage(friendNo: Int, type: ToxMessageType, timeDelta: Int, message: ByteArray) =
        friendMessageHandler(keyFor(friendNo), type, timeDelta, String(message))

    fun friendName(friendNo: Int, newName: ByteArray) =
        friendNameHandler(keyFor(friendNo), String(newName))

    fun fileRecvChunk(friendNo: Int, fileNo: Int, position: Long, data: ByteArray) =
        fileRecvChunkHandler(keyFor(friendNo), fileNo, position, data)

    fun fileRecv(friendNo: Int, fileNo: Int, kind: Int, fileSize: Long, filename: ByteArray) =
        fileRecvHandler(keyFor(friendNo), fileNo, kind, fileSize, String(filename))

    fun friendLossyPacket(friendNo: Int, data: ByteArray) =
        friendLossyPacketHandler(keyFor(friendNo), data)

    fun selfConnectionStatus(connectionStatus: ToxConnection) =
        selfConnectionStatusHandler(connectionStatus.toConnectionStatus())

    fun friendTyping(friendNo: Int, isTyping: Boolean) =
        friendTypingHandler(keyFor(friendNo), isTyping)

    fun fileChunkRequest(friendNo: Int, fileNo: Int, position: Long, length: Int) =
        fileChunkRequestHandler(keyFor(friendNo), fileNo, position, length)

    // JNI Bridge methods
    fun onFriendMessage(friendNo: Int, type: Int, timeDelta: Int, message: ByteArray) =
        friendMessage(friendNo, ToxMessageType.fromInt(type), timeDelta, message)

    fun onFriendRequest(publicKey: ByteArray, timeDelta: Int, message: ByteArray) =
        friendRequest(publicKey, timeDelta, message)

    fun onFriendConnectionStatus(friendNo: Int, status: Int) =
        friendConnectionStatus(friendNo, ToxConnection.fromInt(status))

    fun onSelfConnectionStatus(status: Int) =
        selfConnectionStatus(ToxConnection.fromInt(status))

    fun onFriendStatus(friendNo: Int, status: Int) =
        friendStatus(friendNo, ToxUserStatus.fromInt(status))

    fun onFriendStatusMessage(friendNo: Int, message: ByteArray) =
        friendStatusMessage(friendNo, message)

    fun onFriendName(friendNo: Int, name: ByteArray) =
        friendName(friendNo, name)

    fun onFriendTyping(friendNo: Int, isTyping: Boolean) =
        friendTyping(friendNo, isTyping)

    fun onFriendReadReceipt(friendNo: Int, messageId: Int) =
        friendReadReceipt(friendNo, messageId)

    fun onFileRecv(friendNo: Int, fileNo: Int, kind: Int, fileSize: Long, filename: ByteArray) =
        fileRecv(friendNo, fileNo, kind, fileSize, filename)

    fun onFileRecvControl(friendNo: Int, fileNo: Int, control: Int) =
        fileRecvControl(friendNo, fileNo, ToxFileControl.fromInt(control))

    fun onFileRecvChunk(friendNo: Int, fileNo: Int, position: Long, data: ByteArray) =
        fileRecvChunk(friendNo, fileNo, position, data)

    fun onFileChunkRequest(friendNo: Int, fileNo: Int, position: Long, length: Int) =
        fileChunkRequest(friendNo, fileNo, position, length)

    fun onFriendLosslessPacket(friendNo: Int, data: ByteArray) =
        friendLosslessPacket(friendNo, data)

    fun onFriendLossyPacket(friendNo: Int, data: ByteArray) =
        friendLossyPacket(friendNo, data)
}
