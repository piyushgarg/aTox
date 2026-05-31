package ltd.evilcorp.core.tox.listener

import ltd.evilcorp.domain.core.network.enums.ToxFileControl
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.enums.ToxUserStatus
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupTopicLock
import ltd.evilcorp.domain.core.network.enums.ToxGroupVoiceState
import ltd.evilcorp.domain.core.network.enums.ToxGroupExitType
import ltd.evilcorp.domain.core.network.enums.ToxGroupJoinFail
import ltd.evilcorp.domain.core.network.enums.ToxGroupModEvent
import javax.inject.Inject
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.UserStatus

// Called when a custom lossless packet is received from a friend
typealias FriendLosslessPacketHandler = (publicKey: String, data: ByteArray) -> Unit
// Called when the file transfer state changes (pause, resume, cancel)
typealias FileRecvControlHandler = (publicKey: String, fileNo: Int, control: ToxFileControl) -> Unit
// Called when a friend's status message changes
typealias FriendStatusMessageHandler = (publicKey: String, message: String) -> Unit
// Called when a friend confirms reading a message
typealias FriendReadReceiptHandler = (publicKey: String, messageId: Int) -> Unit
// Called when a friend's online status changes (online, away, do not disturb)
typealias FriendStatusHandler = (publicKey: String, status: UserStatus) -> Unit
// Called when a friend's connection status changes (online/offline)
typealias FriendConnectionStatusHandler = (publicKey: String, status: ConnectionStatus) -> Unit
// Called when a new incoming friend request is received
typealias FriendRequestHandler = (publicKey: String, timeDelta: Int, message: String) -> Unit
// Called when a new private message is received from a friend
typealias FriendMessageHandler = (
    publicKey: String,
    messageType: ToxMessageType,
    timeDelta: Int,
    message: String,
) -> Unit
// Called when a friend's name changes
typealias FriendNameHandler = (publicKey: String, newName: String) -> Unit
// Called when the next chunk (piece) of a file is received
typealias FileRecvChunkHandler = (publicKey: String, fileNo: Int, position: Long, data: ByteArray) -> Unit
// Called when a file transfer request is received from a friend
typealias FileRecvHandler = (publicKey: String, fileNo: Int, kind: Int, size: Long, name: String) -> Unit
// Called when a custom lossy packet is received from a friend
typealias FriendLossyPacketHandler = (publicKey: String, data: ByteArray) -> Unit
// Called when our own DHT network connection status changes (connected/disconnected)
typealias SelfConnectionStatusHandler = (status: ConnectionStatus) -> Unit
// Called when a friend starts or stops typing a message
typealias FriendTypingHandler = (publicKey: String, isTyping: Boolean) -> Unit
// Called when a friend requests a chunk of our outgoing file
typealias FileChunkRequestHandler = (publicKey: String, fileNo: Int, position: Long, length: Int) -> Unit

// Called when an invitation to a group is received
typealias ConferenceInviteHandler = (friendNo: Int, type: Int, cookie: ByteArray) -> Unit
// Called when a new message is received in a group chat
typealias ConferenceMessageHandler = (conferenceNo: Int, peerNo: Int, type: ToxMessageType, message: String) -> Unit
// Called when someone enters or leaves the group
typealias ConferencePeerListChangedHandler = (conferenceNo: Int) -> Unit
// Called when the name of one of the group participants changes
typealias ConferencePeerNameHandler = (conferenceNo: Int, peerNo: Int, newName: String) -> Unit
// Called when the title of the group is changed by one of the participants
typealias ConferenceTitleHandler = (conferenceNo: Int, peerNo: Int, newTitle: String) -> Unit

// Called when an invitation to an NGC group is received
typealias GroupInviteHandler = (friendNo: Int, inviteData: ByteArray, groupName: String) -> Unit
// Called when a new message is received in an NGC group
typealias GroupMessageHandler = (groupNo: Int, peerId: Int, type: ToxMessageType, message: String, messageId: Int) -> Unit
// Called when a participant enters an NGC group
typealias GroupPeerJoinHandler = (groupNo: Int, peerId: Int) -> Unit
// Called when a participant exits an NGC group
typealias GroupPeerExitHandler = (groupNo: Int, peerId: Int, exitType: ToxGroupExitType) -> Unit
// Called when the topic of an NGC group changes
typealias GroupTopicHandler = (groupNo: Int, peerId: Int, topic: String) -> Unit
// Called when the name of one of the NGC group participants changes
typealias GroupPeerNameHandler = (groupNo: Int, peerId: Int, name: String) -> Unit
// Called when the password of an NGC group changes
typealias GroupPasswordHandler = (groupNo: Int, password: ByteArray) -> Unit
// Called when the presence status of a participant in an NGC group changes
typealias GroupPeerStatusHandler = (groupNo: Int, peerId: Int, status: ToxUserStatus) -> Unit
// Called when the privacy state of an NGC group changes
typealias GroupPrivacyStateHandler = (groupNo: Int, privacyState: ToxGroupPrivacyState) -> Unit
// Called when the voice state of an NGC group changes
typealias GroupVoiceStateHandler = (groupNo: Int, voiceState: ToxGroupVoiceState) -> Unit
// Called when the topic lock state of an NGC group changes
typealias GroupTopicLockHandler = (groupNo: Int, topicLock: ToxGroupTopicLock) -> Unit
// Called when the participant limit of an NGC group changes
typealias GroupPeerLimitHandler = (groupNo: Int, peerLimit: Int) -> Unit
// Called when a private message is received inside an NGC group
typealias GroupPrivateMessageHandler = (groupNo: Int, peerId: Int, type: ToxMessageType, message: String, messageId: Int) -> Unit
// Called when the client successfully connects to an NGC group
typealias GroupSelfJoinHandler = (groupNo: Int) -> Unit
// Called when client entry to an NGC group fails
typealias GroupJoinFailHandler = (groupNo: Int, failType: ToxGroupJoinFail) -> Unit
// Called when administrative actions are performed in an NGC group
typealias GroupModerationHandler = (groupNo: Int, sourcePeerId: Int, targetPeerId: Int, modType: ToxGroupModEvent) -> Unit

class ToxEventListener @Inject constructor() {
    var contactMapping: List<Pair<PublicKey, Int>> = listOf()

    private val friendDispatcher = ToxFriendEventDispatcher(this)
    private val fileDispatcher = ToxFileEventDispatcher(this)
    private val groupDispatcher = ToxGroupEventDispatcher(this)

    // Private chat and file transfer event handlers on the Kotlin side
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

    // Conference handler registration on the Kotlin side
    var conferenceInviteHandler: ConferenceInviteHandler = { _, _, _ -> }
    var conferenceMessageHandler: ConferenceMessageHandler = { _, _, _, _ -> }
    var conferencePeerListChangedHandler: ConferencePeerListChangedHandler = { _ -> }
    var conferencePeerNameHandler: ConferencePeerNameHandler = { _, _, _ -> }
    var conferenceTitleHandler: ConferenceTitleHandler = { _, _, _ -> }

    // NGC group handler registration on the Kotlin side
    var groupInviteHandler: GroupInviteHandler = { _, _, _ -> }
    var groupMessageHandler: GroupMessageHandler = { _, _, _, _, _ -> }
    var groupPeerJoinHandler: GroupPeerJoinHandler = { _, _ -> }
    var groupPeerExitHandler: GroupPeerExitHandler = { _, _, _ -> }
    var groupTopicHandler: GroupTopicHandler = { _, _, _ -> }
    var groupPeerNameHandler: GroupPeerNameHandler = { _, _, _ -> }
    var groupPasswordHandler: GroupPasswordHandler = { _, _ -> }
    var groupPeerStatusHandler: GroupPeerStatusHandler = { _, _, _ -> }
    var groupPrivacyStateHandler: GroupPrivacyStateHandler = { _, _ -> }
    var groupVoiceStateHandler: GroupVoiceStateHandler = { _, _ -> }
    var groupTopicLockHandler: GroupTopicLockHandler = { _, _ -> }
    var groupPeerLimitHandler: GroupPeerLimitHandler = { _, _ -> }
    var groupPrivateMessageHandler: GroupPrivateMessageHandler = { _, _, _, _, _ -> }
    var groupSelfJoinHandler: GroupSelfJoinHandler = { _ -> }
    var groupJoinFailHandler: GroupJoinFailHandler = { _, _ -> }
    var groupModerationHandler: GroupModerationHandler = { _, _, _, _ -> }

    private fun keyFor(friendNo: Int) = contactMapping.find { it.second == friendNo }!!.first.string()

    // ===================================================================================
    // JNI Callbacks (Called directly from the native C++ layer NativeToxCore.cpp via JNI)
    // ===================================================================================

    fun onFriendMessage(friendNo: Int, type: Int, timeDelta: Int, message: ByteArray) =
        friendDispatcher.onFriendMessage(keyFor(friendNo), type, timeDelta, message)

    fun onFriendRequest(publicKey: ByteArray, timeDelta: Int, message: ByteArray) =
        friendDispatcher.onFriendRequest(publicKey, timeDelta, message)

    fun onFriendConnectionStatus(friendNo: Int, status: Int) =
        friendDispatcher.onFriendConnectionStatus(keyFor(friendNo), status)

    fun onSelfConnectionStatus(status: Int) =
        friendDispatcher.onSelfConnectionStatus(status)

    fun onFriendStatus(friendNo: Int, status: Int) =
        friendDispatcher.onFriendStatus(keyFor(friendNo), status)

    fun onFriendStatusMessage(friendNo: Int, message: ByteArray) =
        friendDispatcher.onFriendStatusMessage(keyFor(friendNo), message)

    fun onFriendName(friendNo: Int, name: ByteArray) =
        friendDispatcher.onFriendName(keyFor(friendNo), name)

    fun onFriendTyping(friendNo: Int, isTyping: Boolean) =
        friendDispatcher.onFriendTyping(keyFor(friendNo), isTyping)

    fun onFriendReadReceipt(friendNo: Int, messageId: Int) =
        friendDispatcher.onFriendReadReceipt(keyFor(friendNo), messageId)

    fun onFileRecv(friendNo: Int, fileNo: Int, kind: Int, fileSize: Long, filename: ByteArray) =
        fileDispatcher.onFileRecv(keyFor(friendNo), fileNo, kind, fileSize, filename)

    fun onFileRecvControl(friendNo: Int, fileNo: Int, control: Int) =
        fileDispatcher.onFileRecvControl(keyFor(friendNo), fileNo, control)

    fun onFileRecvChunk(friendNo: Int, fileNo: Int, position: Long, data: ByteArray) =
        fileDispatcher.onFileRecvChunk(keyFor(friendNo), fileNo, position, data)

    fun onFileChunkRequest(friendNo: Int, fileNo: Int, position: Long, length: Int) =
        fileDispatcher.onFileChunkRequest(keyFor(friendNo), fileNo, position, length)

    fun onFriendLosslessPacket(friendNo: Int, data: ByteArray) =
        friendDispatcher.onFriendLosslessPacket(keyFor(friendNo), data)

    fun onFriendLossyPacket(friendNo: Int, data: ByteArray) =
        friendDispatcher.onFriendLossyPacket(keyFor(friendNo), data)

    // JNI callbacks for legacy group chats (conferences)
    fun onConferenceInvite(friendNo: Int, type: Int, cookie: ByteArray) =
        groupDispatcher.onConferenceInvite(friendNo, type, cookie)

    fun onConferenceMessage(conferenceNo: Int, peerNo: Int, type: Int, message: ByteArray) =
        groupDispatcher.onConferenceMessage(conferenceNo, peerNo, type, message)

    fun onConferencePeerListChanged(conferenceNo: Int) =
        groupDispatcher.onConferencePeerListChanged(conferenceNo)

    fun onConferencePeerName(conferenceNo: Int, peerNo: Int, name: ByteArray) =
        groupDispatcher.onConferencePeerName(conferenceNo, peerNo, name)

    fun onConferenceTitle(conferenceNo: Int, peerNo: Int, title: ByteArray) =
        groupDispatcher.onConferenceTitle(conferenceNo, peerNo, title)

    // JNI callbacks for NGC groups (called natively)
    fun onGroupInvite(friendNo: Int, inviteData: ByteArray, groupName: ByteArray) =
        groupDispatcher.onGroupInvite(friendNo, inviteData, groupName)

    fun onGroupMessage(groupNo: Int, peerId: Int, type: Int, message: ByteArray, messageId: Int) =
        groupDispatcher.onGroupMessage(groupNo, peerId, type, message, messageId)

    fun onGroupPeerJoin(groupNo: Int, peerId: Int) =
        groupDispatcher.onGroupPeerJoin(groupNo, peerId)

    fun onGroupPeerExit(groupNo: Int, peerId: Int, exitType: Int) =
        groupDispatcher.onGroupPeerExit(groupNo, peerId, exitType)

    fun onGroupTopic(groupNo: Int, peerId: Int, topic: ByteArray) =
        groupDispatcher.onGroupTopic(groupNo, peerId, topic)

    fun onGroupPeerName(groupNo: Int, peerId: Int, name: ByteArray) =
        groupDispatcher.onGroupPeerName(groupNo, peerId, name)

    fun onGroupPassword(groupNo: Int, password: ByteArray) =
        groupDispatcher.onGroupPassword(groupNo, password)

    fun onGroupPeerStatus(groupNo: Int, peerId: Int, status: Int) =
        groupDispatcher.onGroupPeerStatus(groupNo, peerId, status)

    fun onGroupPrivacyState(groupNo: Int, privacyState: Int) =
        groupDispatcher.onGroupPrivacyState(groupNo, privacyState)

    fun onGroupVoiceState(groupNo: Int, voiceState: Int) =
        groupDispatcher.onGroupVoiceState(groupNo, voiceState)

    fun onGroupTopicLock(groupNo: Int, topicLock: Int) =
        groupDispatcher.onGroupTopicLock(groupNo, topicLock)

    fun onGroupPeerLimit(groupNo: Int, peerLimit: Int) =
        groupDispatcher.onGroupPeerLimit(groupNo, peerLimit)

    fun onGroupPrivateMessage(groupNo: Int, peerId: Int, type: Int, message: ByteArray, messageId: Int) =
        groupDispatcher.onGroupPrivateMessage(groupNo, peerId, type, message, messageId)

    fun onGroupSelfJoin(groupNo: Int) =
        groupDispatcher.onGroupSelfJoin(groupNo)

    fun onGroupJoinFail(groupNo: Int, failType: Int) =
        groupDispatcher.onGroupJoinFail(groupNo, failType)

    fun onGroupModeration(groupNo: Int, sourcePeerId: Int, targetPeerId: Int, modType: Int) =
        groupDispatcher.onGroupModeration(groupNo, sourcePeerId, targetPeerId, modType)
}
