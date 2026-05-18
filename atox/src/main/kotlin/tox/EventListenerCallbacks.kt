package ltd.evilcorp.atox.tox

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener

@Singleton
class EventListenerCallbacks @Inject constructor(
    private val friendEventHandler: FriendEventHandler,
    private val fileTransferEventHandler: FileTransferEventHandler,
    private val callEventHandler: CallEventHandler,
) {
    fun setUp(listener: ToxEventListener) = with(listener) {
        friendStatusMessageHandler = friendEventHandler::onFriendStatusMessage
        friendReadReceiptHandler = friendEventHandler::onFriendReadReceipt
        friendStatusHandler = friendEventHandler::onFriendStatus
        friendConnectionStatusHandler = friendEventHandler::onFriendConnectionStatus
        friendRequestHandler = { publicKey, _, message -> friendEventHandler.onFriendRequest(publicKey, message) }
        friendMessageHandler = { publicKey, type, _, message -> friendEventHandler.onFriendMessage(publicKey, type, message) }
        friendNameHandler = friendEventHandler::onFriendName
        fileRecvChunkHandler = fileTransferEventHandler::onFileRecvChunk
        fileRecvHandler = fileTransferEventHandler::onFileRecv
        fileRecvControlHandler = fileTransferEventHandler::onFileRecvControl
        fileChunkRequestHandler = fileTransferEventHandler::onFileChunkRequest
        selfConnectionStatusHandler = friendEventHandler::onSelfConnectionStatus
        friendTypingHandler = friendEventHandler::onFriendTyping
    }

    fun setUp(listener: ToxAvEventListener) = with(listener) {
        callHandler = callEventHandler::onCall
        callStateHandler = callEventHandler::onCallState
        videoBitRateHandler = callEventHandler::onVideoBitRate
        videoReceiveFrameHandler = callEventHandler::onVideoReceiveFrame
        audioBitRateHandler = callEventHandler::onAudioBitRate
        audioReceiveFrameHandler = { _, pcm, channels, samplingRate ->
            callEventHandler.onAudioReceiveFrame(pcm, channels, samplingRate)
        }
    }
}
