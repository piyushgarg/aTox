package ltd.evilcorp.core.tox.listener

import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.enums.ToxUserStatus
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupVoiceState
import ltd.evilcorp.domain.core.network.enums.ToxGroupTopicLock
import ltd.evilcorp.domain.core.network.enums.ToxGroupExitType
import ltd.evilcorp.domain.core.network.enums.ToxGroupJoinFail
import ltd.evilcorp.domain.core.network.enums.ToxGroupModEvent

class ToxGroupEventDispatcher(private val listener: ToxEventListener) {

    // Legacy Conferences JNI Callbacks
    fun onConferenceInvite(friendNo: Int, type: Int, cookie: ByteArray) {
        listener.conferenceInviteHandler(friendNo, type, cookie)
    }

    fun onConferenceMessage(conferenceNo: Int, peerNo: Int, type: Int, message: ByteArray) {
        listener.conferenceMessageHandler(
            conferenceNo,
            peerNo,
            ToxMessageType.fromInt(type),
            String(message)
        )
    }

    fun onConferencePeerListChanged(conferenceNo: Int) {
        listener.conferencePeerListChangedHandler(conferenceNo)
    }

    fun onConferencePeerName(conferenceNo: Int, peerNo: Int, name: ByteArray) {
        listener.conferencePeerNameHandler(conferenceNo, peerNo, String(name))
    }

    fun onConferenceTitle(conferenceNo: Int, peerNo: Int, title: ByteArray) {
        listener.conferenceTitleHandler(conferenceNo, peerNo, String(title))
    }

    // New Generation Group (NGC) JNI Callbacks
    fun onGroupInvite(friendNo: Int, inviteData: ByteArray, groupName: ByteArray) {
        listener.groupInviteHandler(friendNo, inviteData, String(groupName))
    }

    fun onGroupMessage(groupNo: Int, peerId: Int, type: Int, message: ByteArray, messageId: Int) {
        listener.groupMessageHandler(
            groupNo,
            peerId,
            ToxMessageType.fromInt(type),
            String(message),
            messageId
        )
    }

    fun onGroupPeerJoin(groupNo: Int, peerId: Int) {
        listener.groupPeerJoinHandler(groupNo, peerId)
    }

    fun onGroupPeerExit(groupNo: Int, peerId: Int, exitType: Int) {
        listener.groupPeerExitHandler(
            groupNo,
            peerId,
            ToxGroupExitType.fromInt(exitType)
        )
    }

    fun onGroupTopic(groupNo: Int, peerId: Int, topic: ByteArray) {
        listener.groupTopicHandler(groupNo, peerId, String(topic))
    }

    fun onGroupPeerName(groupNo: Int, peerId: Int, name: ByteArray) {
        listener.groupPeerNameHandler(groupNo, peerId, String(name))
    }

    fun onGroupPassword(groupNo: Int, password: ByteArray) {
        listener.groupPasswordHandler(groupNo, password)
    }

    fun onGroupPeerStatus(groupNo: Int, peerId: Int, status: Int) {
        listener.groupPeerStatusHandler(
            groupNo,
            peerId,
            ToxUserStatus.fromInt(status)
        )
    }

    fun onGroupPrivacyState(groupNo: Int, privacyState: Int) {
        listener.groupPrivacyStateHandler(
            groupNo,
            ToxGroupPrivacyState.fromInt(privacyState)
        )
    }

    fun onGroupVoiceState(groupNo: Int, voiceState: Int) {
        listener.groupVoiceStateHandler(
            groupNo,
            ToxGroupVoiceState.fromInt(voiceState)
        )
    }

    fun onGroupTopicLock(groupNo: Int, topicLock: Int) {
        listener.groupTopicLockHandler(
            groupNo,
            ToxGroupTopicLock.fromInt(topicLock)
        )
    }

    fun onGroupPeerLimit(groupNo: Int, peerLimit: Int) {
        listener.groupPeerLimitHandler(groupNo, peerLimit)
    }

    fun onGroupPrivateMessage(groupNo: Int, peerId: Int, type: Int, message: ByteArray, messageId: Int) {
        listener.groupPrivateMessageHandler(
            groupNo,
            peerId,
            ToxMessageType.fromInt(type),
            String(message),
            messageId
        )
    }

    fun onGroupSelfJoin(groupNo: Int) {
        listener.groupSelfJoinHandler(groupNo)
    }

    fun onGroupJoinFail(groupNo: Int, failType: Int) {
        listener.groupJoinFailHandler(
            groupNo,
            ToxGroupJoinFail.fromInt(failType)
        )
    }

    fun onGroupModeration(groupNo: Int, sourcePeerId: Int, targetPeerId: Int, modType: Int) {
        listener.groupModerationHandler(
            groupNo,
            sourcePeerId,
            targetPeerId,
            ToxGroupModEvent.fromInt(modType)
        )
    }
}
