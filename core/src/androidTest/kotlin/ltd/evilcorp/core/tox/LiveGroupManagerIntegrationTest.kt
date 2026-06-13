// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.runtime.ToxWrapper
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LiveGroupManagerIntegrationTest {

    @Test
    fun testLiveGroupCreationInvitationAndMessaging() = runTest {
        val options = SaveOptions(null, false, ProxyType.None, "", 0)

        // 1. Initialize Alice and Bob native instances
        val listenerA = ToxEventListener()
        val avListenerA = ToxAvEventListener()
        val toxA = ToxWrapper(listenerA, avListenerA, options)

        val listenerB = ToxEventListener()
        val avListenerB = ToxAvEventListener()
        val toxB = ToxWrapper(listenerB, avListenerB, options)

        try {
            toxA.setName("Alice")
            toxB.setName("Bob")

            val pkA = toxA.getPublicKey()
            val pkB = toxB.getPublicKey()

            // Establish JNI connection
            val friendNoB = toxA.addFriendNoRequest(pkB)
            assertTrue(friendNoB >= 0, "Alice should add Bob")

            val friendNoA = toxB.addFriendNoRequest(pkA)
            assertTrue(friendNoA >= 0, "Bob should add Alice")

            val isAConnected = AtomicBoolean(false)
            val isBConnected = AtomicBoolean(false)

            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk.lowercase() == pkB.string().lowercase() && status != ConnectionStatus.None) {
                    isAConnected.set(true)
                }
            }

            listenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk.lowercase() == pkA.string().lowercase() && status != ConnectionStatus.None) {
                    isBConnected.set(true)
                }
            }

            // Set up invite listener on Bob
            val bobGroupNo = AtomicInteger(-1)
            val inviteReceived = AtomicBoolean(false)
            listenerB.groupInviteHandler = { friendNo, inviteData, groupName ->
                inviteReceived.set(true)
                val gn = toxB.groupJoin(friendNo, inviteData, "Bob".toByteArray(), null)
                bobGroupNo.set(gn)
            }

            // Set up group message listener on Alice
            val groupMessageReceived = AtomicBoolean(false)
            var receivedMessageText = ""
            listenerA.groupMessageHandler = { groupNo, peerId, type, message, messageId ->
                receivedMessageText = message
                groupMessageReceived.set(true)
            }

            // Start native iterate loops
            val runLoops = AtomicBoolean(true)
            val jobA = launch {
                while (runLoops.get()) {
                    try {
                        toxA.iterate()
                        delay(toxA.iterationInterval().coerceAtLeast(10L))
                    } catch (e: Exception) {
                        break
                    }
                }
            }
            val jobB = launch {
                while (runLoops.get()) {
                    try {
                        toxB.iterate()
                        delay(toxB.iterationInterval().coerceAtLeast(10L))
                    } catch (e: Exception) {
                        break
                    }
                }
            }

            // Bootstrap
            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.selfGetDhtId())

            // Wait up to 5 seconds to establish local connection
            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 5000L) {
                delay(100)
            }

            assertTrue(isAConnected.get(), "Alice should connect to Bob")
            assertTrue(isBConnected.get(), "Bob should connect to Alice")

            // 2. Alice creates NGC Group
            val groupNoA = toxA.groupNew(
                ToxGroupPrivacyState.PUBLIC,
                "Atox NGC Group".toByteArray(),
                "Alice".toByteArray()
            )
            assertTrue(groupNoA >= 0, "Alice should successfully create NGC group")

            // Wait a moment for group to stabilize
            delay(500)

            // Alice sends group invite to Bob (friendNoB is 0)
            val inviteSent = toxA.groupInviteSend(groupNoA, friendNoB)
            assertTrue(inviteSent, "Alice should send group invite to Bob")

            // Wait for Bob to receive group invite and auto-join
            val inviteTime = System.currentTimeMillis()
            while (!inviteReceived.get() && (System.currentTimeMillis() - inviteTime) < 3000L) {
                delay(100)
            }
            assertTrue(inviteReceived.get(), "Bob should receive group invite")
            assertTrue(bobGroupNo.get() >= 0, "Bob should successfully join group")

            // Wait for NGC peers routing and syncing
            delay(2000)

            // 3. Bob sends group message
            val testMsg = "Hello group members from Bob!"
            var groupMsgSent = false
            val sendStartTime = System.currentTimeMillis()
            while (!groupMsgSent && (System.currentTimeMillis() - sendStartTime) < 3000L) {
                val res = toxB.groupSendMessage(bobGroupNo.get(), ToxMessageType.NORMAL, testMsg.toByteArray(Charsets.UTF_8))
                groupMsgSent = res >= 0
                if (!groupMsgSent) delay(200)
            }
            assertTrue(groupMsgSent, "Bob should send group message")

            // Wait for Alice to receive group message
            val msgStartTime = System.currentTimeMillis()
            while (!groupMessageReceived.get() && (System.currentTimeMillis() - msgStartTime) < 3000L) {
                delay(100)
            }
            assertTrue(groupMessageReceived.get(), "Alice should receive group message from Bob")
            assertEquals(testMsg, receivedMessageText)

            // Clean up loops
            runLoops.set(false)
            jobA.join()
            jobB.join()

        } finally {
            toxA.close()
            toxB.close()
        }
    }
}
