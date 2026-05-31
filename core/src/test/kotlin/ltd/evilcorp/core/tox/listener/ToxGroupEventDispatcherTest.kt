package ltd.evilcorp.core.tox.listener

import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.enums.ToxUserStatus
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupVoiceState
import ltd.evilcorp.domain.core.network.enums.ToxGroupTopicLock
import ltd.evilcorp.domain.core.network.enums.ToxGroupExitType
import ltd.evilcorp.domain.core.network.enums.ToxGroupJoinFail
import ltd.evilcorp.domain.core.network.enums.ToxGroupModEvent
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToxGroupEventDispatcherTest {

    @Test
    fun testAllDispatchedLegacyConferenceEvents() {
        val listener = ToxEventListener()
        val dispatcher = ToxGroupEventDispatcher(listener)

        var inviteCalled = false
        listener.conferenceInviteHandler = { friendNo, type, cookie ->
            assertEquals(1, friendNo)
            assertEquals(2, type)
            assertTrue(byteArrayOf(5, 6).contentEquals(cookie))
            inviteCalled = true
        }
        dispatcher.onConferenceInvite(1, 2, byteArrayOf(5, 6))
        assertTrue(inviteCalled)

        var messageCalled = false
        listener.conferenceMessageHandler = { conferenceNo, peerNo, type, message ->
            assertEquals(10, conferenceNo)
            assertEquals(20, peerNo)
            assertEquals(ToxMessageType.ACTION, type)
            assertEquals("waved hello", message)
            messageCalled = true
        }
        dispatcher.onConferenceMessage(10, 20, 1, "waved hello".toByteArray()) // 1 = ACTION
        assertTrue(messageCalled)

        var peerListCalled = false
        listener.conferencePeerListChangedHandler = { conferenceNo ->
            assertEquals(10, conferenceNo)
            peerListCalled = true
        }
        dispatcher.onConferencePeerListChanged(10)
        assertTrue(peerListCalled)

        var peerNameCalled = false
        listener.conferencePeerNameHandler = { conferenceNo, peerNo, newName ->
            assertEquals(10, conferenceNo)
            assertEquals(20, peerNo)
            assertEquals("Charlie", newName)
            peerNameCalled = true
        }
        dispatcher.onConferencePeerName(10, 20, "Charlie".toByteArray())
        assertTrue(peerNameCalled)

        var titleCalled = false
        listener.conferenceTitleHandler = { conferenceNo, peerNo, newTitle ->
            assertEquals(10, conferenceNo)
            assertEquals(20, peerNo)
            assertEquals("New Conference Title", newTitle)
            titleCalled = true
        }
        dispatcher.onConferenceTitle(10, 20, "New Conference Title".toByteArray())
        assertTrue(titleCalled)
    }

    @Test
    fun testAllDispatchedNgcGroupEvents() {
        val listener = ToxEventListener()
        val dispatcher = ToxGroupEventDispatcher(listener)

        var groupInviteCalled = false
        listener.groupInviteHandler = { friendNo, inviteData, groupName ->
            assertEquals(1, friendNo)
            assertTrue(byteArrayOf(10, 11).contentEquals(inviteData))
            assertEquals("My Group", groupName)
            groupInviteCalled = true
        }
        dispatcher.onGroupInvite(1, byteArrayOf(10, 11), "My Group".toByteArray())
        assertTrue(groupInviteCalled)

        var groupMessageCalled = false
        listener.groupMessageHandler = { groupNo, peerId, type, message, messageId ->
            assertEquals(2, groupNo)
            assertEquals(3, peerId)
            assertEquals(ToxMessageType.NORMAL, type)
            assertEquals("Group msg", message)
            assertEquals(55, messageId)
            groupMessageCalled = true
        }
        dispatcher.onGroupMessage(2, 3, 0, "Group msg".toByteArray(), 55)
        assertTrue(groupMessageCalled)

        var groupPeerJoinCalled = false
        listener.groupPeerJoinHandler = { groupNo, peerId ->
            assertEquals(2, groupNo)
            assertEquals(3, peerId)
            groupPeerJoinCalled = true
        }
        dispatcher.onGroupPeerJoin(2, 3)
        assertTrue(groupPeerJoinCalled)

        var groupPeerExitCalled = false
        listener.groupPeerExitHandler = { groupNo, peerId, exitType ->
            assertEquals(2, groupNo)
            assertEquals(3, peerId)
            assertEquals(ToxGroupExitType.KICK, exitType)
            groupPeerExitCalled = true
        }
        dispatcher.onGroupPeerExit(2, 3, 4) // 4 = Kick in ToxGroupExitType
        assertTrue(groupPeerExitCalled)

        var groupTopicCalled = false
        listener.groupTopicHandler = { groupNo, peerId, topic ->
            assertEquals(2, groupNo)
            assertEquals(3, peerId)
            assertEquals("Cool Topic", topic)
            groupTopicCalled = true
        }
        dispatcher.onGroupTopic(2, 3, "Cool Topic".toByteArray())
        assertTrue(groupTopicCalled)

        var groupPeerNameCalled = false
        listener.groupPeerNameHandler = { groupNo, peerId, name ->
            assertEquals(2, groupNo)
            assertEquals(3, peerId)
            assertEquals("Bobby", name)
            groupPeerNameCalled = true
        }
        dispatcher.onGroupPeerName(2, 3, "Bobby".toByteArray())
        assertTrue(groupPeerNameCalled)

        var groupPasswordCalled = false
        listener.groupPasswordHandler = { groupNo, password ->
            assertEquals(2, groupNo)
            assertTrue(byteArrayOf(9, 9).contentEquals(password))
            groupPasswordCalled = true
        }
        dispatcher.onGroupPassword(2, byteArrayOf(9, 9))
        assertTrue(groupPasswordCalled)

        var groupPeerStatusCalled = false
        listener.groupPeerStatusHandler = { groupNo, peerId, status ->
            assertEquals(2, groupNo)
            assertEquals(3, peerId)
            assertEquals(ToxUserStatus.BUSY, status)
            groupPeerStatusCalled = true
        }
        dispatcher.onGroupPeerStatus(2, 3, 2) // 2 = Busy
        assertTrue(groupPeerStatusCalled)

        var groupPrivacyCalled = false
        listener.groupPrivacyStateHandler = { groupNo, privacyState ->
            assertEquals(2, groupNo)
            assertEquals(ToxGroupPrivacyState.PRIVATE, privacyState)
            groupPrivacyCalled = true
        }
        dispatcher.onGroupPrivacyState(2, 1) // 1 = Private
        assertTrue(groupPrivacyCalled)

        var groupVoiceCalled = false
        listener.groupVoiceStateHandler = { groupNo, voiceState ->
            assertEquals(2, groupNo)
            assertEquals(ToxGroupVoiceState.MODERATOR, voiceState)
            groupVoiceCalled = true
        }
        dispatcher.onGroupVoiceState(2, 1) // 1 = Moderator
        assertTrue(groupVoiceCalled)

        var groupTopicLockCalled = false
        listener.groupTopicLockHandler = { groupNo, topicLock ->
            assertEquals(2, groupNo)
            assertEquals(ToxGroupTopicLock.ENABLED, topicLock)
            groupTopicLockCalled = true
        }
        dispatcher.onGroupTopicLock(2, 0) // 0 = Enabled
        assertTrue(groupTopicLockCalled)

        var groupPeerLimitCalled = false
        listener.groupPeerLimitHandler = { groupNo, peerLimit ->
            assertEquals(2, groupNo)
            assertEquals(100, peerLimit)
            groupPeerLimitCalled = true
        }
        dispatcher.onGroupPeerLimit(2, 100)
        assertTrue(groupPeerLimitCalled)

        var privateMessageCalled = false
        listener.groupPrivateMessageHandler = { groupNo, peerId, type, message, messageId ->
            assertEquals(2, groupNo)
            assertEquals(3, peerId)
            assertEquals(ToxMessageType.NORMAL, type)
            assertEquals("Secret", message)
            assertEquals(66, messageId)
            privateMessageCalled = true
        }
        dispatcher.onGroupPrivateMessage(2, 3, 0, "Secret".toByteArray(), 66)
        assertTrue(privateMessageCalled)

        var selfJoinCalled = false
        listener.groupSelfJoinHandler = { groupNo ->
            assertEquals(2, groupNo)
            selfJoinCalled = true
        }
        dispatcher.onGroupSelfJoin(2)
        assertTrue(selfJoinCalled)

        var joinFailCalled = false
        listener.groupJoinFailHandler = { groupNo, failType ->
            assertEquals(2, groupNo)
            assertEquals(ToxGroupJoinFail.INVALID_PASSWORD, failType)
            joinFailCalled = true
        }
        dispatcher.onGroupJoinFail(2, 1) // 1 = Invalid Password
        assertTrue(joinFailCalled)

        var groupModerationCalled = false
        listener.groupModerationHandler = { groupNo, source, target, modType ->
            assertEquals(2, groupNo)
            assertEquals(10, source)
            assertEquals(11, target)
            assertEquals(ToxGroupModEvent.KICK, modType)
            groupModerationCalled = true
        }
        dispatcher.onGroupModeration(2, 10, 11, 0) // 0 = Kick
        assertTrue(groupModerationCalled)
    }
}
