package ltd.evilcorp.core.tox.listener

import ltd.evilcorp.domain.core.network.enums.ToxConnection
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.enums.ToxUserStatus
import ltd.evilcorp.domain.core.network.bytesToHex

class ToxFriendEventDispatcher(private val listener: ToxEventListener) {

    fun onFriendMessage(publicKey: String, type: Int, timeDelta: Int, message: ByteArray) {
        listener.friendMessageHandler(
            publicKey,
            ToxMessageType.fromInt(type),
            timeDelta,
            String(message)
        )
    }

    fun onFriendRequest(publicKey: ByteArray, timeDelta: Int, message: ByteArray) {
        listener.friendRequestHandler(
            publicKey.bytesToHex(),
            timeDelta,
            String(message)
        )
    }

    fun onFriendConnectionStatus(publicKey: String, status: Int) {
        listener.friendConnectionStatusHandler(
            publicKey,
            ToxConnection.fromInt(status).toConnectionStatus()
        )
    }

    fun onSelfConnectionStatus(status: Int) {
        listener.selfConnectionStatusHandler(
            ToxConnection.fromInt(status).toConnectionStatus()
        )
    }

    fun onFriendStatus(publicKey: String, status: Int) {
        listener.friendStatusHandler(
            publicKey,
            ToxUserStatus.fromInt(status).toUserStatus()
        )
    }

    fun onFriendStatusMessage(publicKey: String, message: ByteArray) {
        listener.friendStatusMessageHandler(
            publicKey,
            String(message)
        )
    }

    fun onFriendName(publicKey: String, name: ByteArray) {
        listener.friendNameHandler(
            publicKey,
            String(name)
        )
    }

    fun onFriendTyping(publicKey: String, isTyping: Boolean) {
        listener.friendTypingHandler(
            publicKey,
            isTyping
        )
    }

    fun onFriendReadReceipt(publicKey: String, messageId: Int) {
        listener.friendReadReceiptHandler(
            publicKey,
            messageId
        )
    }

    fun onFriendLosslessPacket(publicKey: String, data: ByteArray) {
        listener.friendLosslessPacketHandler(
            publicKey,
            data
        )
    }

    fun onFriendLossyPacket(publicKey: String, data: ByteArray) {
        listener.friendLossyPacketHandler(
            publicKey,
            data
        )
    }
}
