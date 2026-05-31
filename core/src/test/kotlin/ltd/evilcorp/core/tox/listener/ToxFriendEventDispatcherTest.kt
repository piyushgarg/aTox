package ltd.evilcorp.core.tox.listener

import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToxFriendEventDispatcherTest {

    @Test
    fun testAllDispatchedFriendEvents() {
        val listener = ToxEventListener()
        val dispatcher = ToxFriendEventDispatcher(listener)

        var friendMessageCalled = false
        listener.friendMessageHandler = { publicKey, messageType, timeDelta, message ->
            assertEquals("friend_pk", publicKey)
            assertEquals(ToxMessageType.NORMAL, messageType)
            assertEquals(5, timeDelta)
            assertEquals("Hello", message)
            friendMessageCalled = true
        }
        dispatcher.onFriendMessage("friend_pk", 0, 5, "Hello".toByteArray())
        assertTrue(friendMessageCalled)

        var friendRequestCalled = false
        listener.friendRequestHandler = { publicKey, timeDelta, message ->
            assertEquals("01020304", publicKey.uppercase())
            assertEquals(10, timeDelta)
            assertEquals("Add me", message)
            friendRequestCalled = true
        }
        dispatcher.onFriendRequest(byteArrayOf(1, 2, 3, 4), 10, "Add me".toByteArray())
        assertTrue(friendRequestCalled)

        var friendConnectionStatusCalled = false
        listener.friendConnectionStatusHandler = { publicKey, status ->
            assertEquals("friend_pk", publicKey)
            assertEquals(ConnectionStatus.UDP, status)
            friendConnectionStatusCalled = true
        }
        dispatcher.onFriendConnectionStatus("friend_pk", 2) // 2 = UDP in JNI ToxConnection
        assertTrue(friendConnectionStatusCalled)

        var selfConnectionStatusCalled = false
        listener.selfConnectionStatusHandler = { status ->
            assertEquals(ConnectionStatus.TCP, status)
            selfConnectionStatusCalled = true
        }
        dispatcher.onSelfConnectionStatus(1) // 1 = TCP in JNI ToxConnection
        assertTrue(selfConnectionStatusCalled)

        var friendStatusCalled = false
        listener.friendStatusHandler = { publicKey, status ->
            assertEquals("friend_pk", publicKey)
            assertEquals(UserStatus.Away, status)
            friendStatusCalled = true
        }
        dispatcher.onFriendStatus("friend_pk", 1) // 1 = Away in JNI ToxUserStatus
        assertTrue(friendStatusCalled)

        var friendStatusMessageCalled = false
        listener.friendStatusMessageHandler = { publicKey, message ->
            assertEquals("friend_pk", publicKey)
            assertEquals("Out to lunch", message)
            friendStatusMessageCalled = true
        }
        dispatcher.onFriendStatusMessage("friend_pk", "Out to lunch".toByteArray())
        assertTrue(friendStatusMessageCalled)

        var friendNameCalled = false
        listener.friendNameHandler = { publicKey, name ->
            assertEquals("friend_pk", publicKey)
            assertEquals("Alice", name)
            friendNameCalled = true
        }
        dispatcher.onFriendName("friend_pk", "Alice".toByteArray())
        assertTrue(friendNameCalled)

        var friendTypingCalled = false
        listener.friendTypingHandler = { publicKey, isTyping ->
            assertEquals("friend_pk", publicKey)
            assertTrue(isTyping)
            friendTypingCalled = true
        }
        dispatcher.onFriendTyping("friend_pk", true)
        assertTrue(friendTypingCalled)

        var friendReadReceiptCalled = false
        listener.friendReadReceiptHandler = { publicKey, messageId ->
            assertEquals("friend_pk", publicKey)
            assertEquals(42, messageId)
            friendReadReceiptCalled = true
        }
        dispatcher.onFriendReadReceipt("friend_pk", 42)
        assertTrue(friendReadReceiptCalled)

        var losslessCalled = false
        listener.friendLosslessPacketHandler = { publicKey, data ->
            assertEquals("friend_pk", publicKey)
            assertTrue(byteArrayOf(1, 2).contentEquals(data))
            losslessCalled = true
        }
        dispatcher.onFriendLosslessPacket("friend_pk", byteArrayOf(1, 2))
        assertTrue(losslessCalled)

        var lossyCalled = false
        listener.friendLossyPacketHandler = { publicKey, data ->
            assertEquals("friend_pk", publicKey)
            assertTrue(byteArrayOf(3, 4).contentEquals(data))
            lossyCalled = true
        }
        dispatcher.onFriendLossyPacket("friend_pk", byteArrayOf(3, 4))
        assertTrue(lossyCalled)
    }
}
