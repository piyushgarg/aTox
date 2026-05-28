package ltd.evilcorp.atox.infrastructure.tox

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ltd.evilcorp.domain.core.network.enums.ToxGroupExitType
import ltd.evilcorp.domain.core.network.enums.ToxGroupJoinFail
import ltd.evilcorp.domain.core.network.enums.ToxGroupModEvent
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxMessageType

@Singleton
class GroupEventHandler @Inject constructor() {
    private val _groupEvents = MutableSharedFlow<GroupJniEvent>(extraBufferCapacity = 128)
    val groupEvents: SharedFlow<GroupJniEvent> = _groupEvents.asSharedFlow()

    fun onGroupInvite(friendNo: Int, inviteData: ByteArray, groupName: String) {
        _groupEvents.tryEmit(GroupJniEvent.GroupInvite(friendNo, inviteData, groupName))
    }

    fun onGroupMessage(
        groupNo: Int,
        peerId: Int,
        type: ToxMessageType,
        message: String,
        messageId: Int,
    ) {
        _groupEvents.tryEmit(GroupJniEvent.GroupMessage(groupNo, peerId, type, message, messageId))
    }

    fun onGroupPeerJoin(groupNo: Int, peerId: Int) {
        _groupEvents.tryEmit(GroupJniEvent.GroupPeerJoin(groupNo, peerId))
    }

    fun onGroupPeerExit(groupNo: Int, peerId: Int, exitType: ToxGroupExitType) {
        _groupEvents.tryEmit(GroupJniEvent.GroupPeerExit(groupNo, peerId, exitType))
    }

    fun onGroupTopic(groupNo: Int, peerId: Int, topic: String) {
        _groupEvents.tryEmit(GroupJniEvent.GroupTopic(groupNo, peerId, topic))
    }

    fun onGroupPeerName(groupNo: Int, peerId: Int, name: String) {
        _groupEvents.tryEmit(GroupJniEvent.GroupPeerName(groupNo, peerId, name))
    }

    fun onGroupPassword(groupNo: Int, password: ByteArray) {
        _groupEvents.tryEmit(GroupJniEvent.GroupPassword(groupNo, password))
    }

    fun onGroupPeerStatus(groupNo: Int, peerId: Int, status: ltd.evilcorp.domain.core.network.enums.ToxUserStatus) {
        _groupEvents.tryEmit(GroupJniEvent.GroupPeerStatus(groupNo, peerId, status))
    }

    fun onGroupPrivacyState(groupNo: Int, privacyState: ToxGroupPrivacyState) {
        _groupEvents.tryEmit(GroupJniEvent.GroupPrivacyStateChanged(groupNo, privacyState))
    }

    fun onGroupVoiceState(groupNo: Int, voiceState: ltd.evilcorp.domain.core.network.enums.ToxGroupVoiceState) {
        _groupEvents.tryEmit(GroupJniEvent.GroupVoiceState(groupNo, voiceState))
    }

    fun onGroupTopicLock(groupNo: Int, topicLock: ltd.evilcorp.domain.core.network.enums.ToxGroupTopicLock) {
        _groupEvents.tryEmit(GroupJniEvent.GroupTopicLock(groupNo, topicLock))
    }

    fun onGroupPeerLimit(groupNo: Int, peerLimit: Int) {
        _groupEvents.tryEmit(GroupJniEvent.GroupPeerLimit(groupNo, peerLimit))
    }

    fun onGroupPrivateMessage(groupNo: Int, peerId: Int, type: ToxMessageType, message: String) {
        _groupEvents.tryEmit(GroupJniEvent.GroupPrivateMessage(groupNo, peerId, type, message))
    }

    fun onGroupConnected(groupNo: Int) {
        _groupEvents.tryEmit(GroupJniEvent.GroupConnected(groupNo))
    }

    fun onGroupJoinFail(groupNo: Int, joinFail: ToxGroupJoinFail) {
        _groupEvents.tryEmit(GroupJniEvent.GroupJoinFail(groupNo, joinFail))
    }

    fun onGroupModeration(groupNo: Int, sourcePeerId: Int, targetPeerId: Int, modEvent: ToxGroupModEvent) {
        _groupEvents.tryEmit(GroupJniEvent.GroupModeration(groupNo, sourcePeerId, targetPeerId, modEvent))
    }
}
