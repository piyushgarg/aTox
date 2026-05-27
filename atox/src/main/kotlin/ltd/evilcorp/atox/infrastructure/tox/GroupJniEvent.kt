package ltd.evilcorp.atox.infrastructure.tox

import ltd.evilcorp.domain.tox.enums.ToxGroupExitType
import ltd.evilcorp.domain.tox.enums.ToxGroupJoinFail
import ltd.evilcorp.domain.tox.enums.ToxGroupModEvent
import ltd.evilcorp.domain.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.tox.enums.ToxGroupRole
import ltd.evilcorp.domain.tox.enums.ToxMessageType
import ltd.evilcorp.domain.tox.enums.ToxUserStatus
import ltd.evilcorp.domain.tox.enums.ToxGroupVoiceState
import ltd.evilcorp.domain.tox.enums.ToxGroupTopicLock

sealed interface GroupJniEvent {
    data class GroupInvite(val friendNo: Int, val inviteData: ByteArray, val groupName: String) : GroupJniEvent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as GroupInvite
            if (friendNo != other.friendNo) return false
            if (!inviteData.contentEquals(other.inviteData)) return false
            if (groupName != other.groupName) return false
            return true
        }

        override fun hashCode(): Int {
            var result = friendNo
            result = 31 * result + inviteData.contentHashCode()
            result = 31 * result + groupName.hashCode()
            return result
        }
    }

    data class GroupMessage(
        val groupNo: Int,
        val peerId: Int,
        val type: ToxMessageType,
        val message: String,
        val messageId: Int
    ) : GroupJniEvent

    data class GroupPeerJoin(val groupNo: Int, val peerId: Int) : GroupJniEvent

    data class GroupPeerExit(val groupNo: Int, val peerId: Int, val exitType: ToxGroupExitType) : GroupJniEvent

    data class GroupTopic(val groupNo: Int, val peerId: Int, val topic: String) : GroupJniEvent

    data class GroupPeerName(val groupNo: Int, val peerId: Int, val name: String) : GroupJniEvent

    data class GroupPassword(val groupNo: Int, val password: ByteArray) : GroupJniEvent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as GroupPassword
            if (groupNo != other.groupNo) return false
            if (!password.contentEquals(other.password)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = groupNo
            result = 31 * result + password.contentHashCode()
            return result
        }
    }

    data class GroupPeerStatus(val groupNo: Int, val peerId: Int, val status: ToxUserStatus) : GroupJniEvent

    data class GroupPrivacyStateChanged(val groupNo: Int, val privacyState: ToxGroupPrivacyState) : GroupJniEvent

    data class GroupVoiceState(val groupNo: Int, val voiceState: ToxGroupVoiceState) : GroupJniEvent

    data class GroupTopicLock(val groupNo: Int, val topicLock: ToxGroupTopicLock) : GroupJniEvent

    data class GroupPeerLimit(val groupNo: Int, val peerLimit: Int) : GroupJniEvent

    data class GroupPrivateMessage(val groupNo: Int, val peerId: Int, val type: ToxMessageType, val message: String) : GroupJniEvent

    data class GroupConnected(val groupNo: Int) : GroupJniEvent

    data class GroupJoinFail(val groupNo: Int, val joinFail: ToxGroupJoinFail) : GroupJniEvent

    data class GroupModeration(val groupNo: Int, val sourcePeerId: Int, val targetPeerId: Int, val modEvent: ToxGroupModEvent) : GroupJniEvent
}
