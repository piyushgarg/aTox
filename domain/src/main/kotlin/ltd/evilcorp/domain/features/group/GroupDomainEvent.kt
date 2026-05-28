package ltd.evilcorp.domain.features.group

import ltd.evilcorp.domain.core.network.enums.ToxGroupExitType
import ltd.evilcorp.domain.core.network.enums.ToxGroupJoinFail
import ltd.evilcorp.domain.core.network.enums.ToxGroupModEvent
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxMessageType

sealed class GroupDomainEvent {
    data class GroupInvite(val friendNo: Int, val inviteData: ByteArray, val groupName: String) : GroupDomainEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is GroupInvite) return false
            return friendNo == other.friendNo && inviteData.contentEquals(other.inviteData) && groupName == other.groupName
        }
        override fun hashCode(): Int {
            var result = friendNo
            result = 31 * result + inviteData.contentHashCode()
            result = 31 * result + groupName.hashCode()
            return result
        }
    }

    data class GroupMessage(val groupNo: Int, val peerId: Int, val type: ToxMessageType, val message: String, val messageId: Int) : GroupDomainEvent()
    data class GroupPeerJoin(val groupNo: Int, val peerId: Int) : GroupDomainEvent()
    data class GroupPeerExit(val groupNo: Int, val peerId: Int, val exitType: ToxGroupExitType) : GroupDomainEvent()
    data class GroupTopic(val groupNo: Int, val peerId: Int, val topic: String) : GroupDomainEvent()
    data class GroupPeerName(val groupNo: Int, val peerId: Int, val name: String) : GroupDomainEvent()
    data class GroupPassword(val groupNo: Int, val password: ByteArray) : GroupDomainEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is GroupPassword) return false
            return groupNo == other.groupNo && password.contentEquals(other.password)
        }
        override fun hashCode(): Int {
            var result = groupNo
            result = 31 * result + password.contentHashCode()
            return result
        }
    }
    data class GroupPeerStatus(val groupNo: Int, val peerId: Int, val status: Int) : GroupDomainEvent()
    data class GroupPrivacyStateChanged(val groupNo: Int, val privacyState: ToxGroupPrivacyState) : GroupDomainEvent()
    data class GroupVoiceState(val groupNo: Int, val voiceState: Int) : GroupDomainEvent()
    data class GroupTopicLock(val groupNo: Int, val topicLock: Int) : GroupDomainEvent()
    data class GroupPeerLimit(val groupNo: Int, val peerLimit: Int) : GroupDomainEvent()
    data class GroupPrivateMessage(val groupNo: Int, val peerId: Int, val type: ToxMessageType, val message: String) : GroupDomainEvent()
    data class GroupConnected(val groupNo: Int) : GroupDomainEvent()
    data class GroupJoinFail(val groupNo: Int, val joinFail: ToxGroupJoinFail) : GroupDomainEvent()
    data class GroupModeration(val groupNo: Int, val sourcePeerId: Int, val targetPeerId: Int, val modEvent: ToxGroupModEvent) : GroupDomainEvent()
}
