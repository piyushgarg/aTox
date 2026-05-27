package ltd.evilcorp.atox.infrastructure.tox

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.domain.tox.enums.ToxMessageType

@Singleton
class EventListenerCallbacks @Inject constructor(
    private val friendEventHandler: FriendEventHandler,
    private val fileTransferEventHandler: FileTransferEventHandler,
    private val callEventHandler: CallEventHandler,
    private val groupEventHandler: GroupEventHandler,
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

        groupInviteHandler = groupEventHandler::onGroupInvite
        groupMessageHandler = { groupNo, peerId, type, message, messageId ->
            groupEventHandler.onGroupMessage(groupNo, peerId, type, message, messageId)
        }
        groupPeerJoinHandler = groupEventHandler::onGroupPeerJoin
        groupPeerExitHandler = groupEventHandler::onGroupPeerExit
        groupTopicHandler = groupEventHandler::onGroupTopic
        groupPeerNameHandler = groupEventHandler::onGroupPeerName
        groupPasswordHandler = groupEventHandler::onGroupPassword
        groupPeerStatusHandler = groupEventHandler::onGroupPeerStatus
        groupPrivacyStateHandler = groupEventHandler::onGroupPrivacyState
        groupVoiceStateHandler = groupEventHandler::onGroupVoiceState
        groupTopicLockHandler = groupEventHandler::onGroupTopicLock
        groupPeerLimitHandler = groupEventHandler::onGroupPeerLimit
        groupPrivateMessageHandler = { groupNo, peerId, type, message, _ ->
            groupEventHandler.onGroupPrivateMessage(groupNo, peerId, type, message)
        }
        groupSelfJoinHandler = groupEventHandler::onGroupConnected
        groupJoinFailHandler = groupEventHandler::onGroupJoinFail
        groupModerationHandler = groupEventHandler::onGroupModeration
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
