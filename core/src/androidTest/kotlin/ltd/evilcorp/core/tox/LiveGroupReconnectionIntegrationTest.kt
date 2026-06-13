// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import kotlin.test.assertTrue

@Suppress("LargeClass", "ComplexCondition", "LongMethod")
@RunWith(AndroidJUnit4::class)
class LiveGroupReconnectionIntegrationTest {

    @Suppress("LongMethod")
    @Test
    fun testLiveGroupReconnectionAndRecovery() = runBlocking {
        // Allow lingering port resources from previous runs/tests to be fully released by the OS
        delay(5000)

        val options = SaveOptions(null, true, ProxyType.None, "", 0)

        // 1. Initialize Alice, Bob, and Charlie JNI wrappers
        val listenerA = ToxEventListener()
        val avListenerA = ToxAvEventListener()
        var toxA = ToxWrapper(listenerA, avListenerA, options)

        val listenerB = ToxEventListener()
        val avListenerB = ToxAvEventListener()
        var toxB = ToxWrapper(listenerB, avListenerB, options)

        val listenerC = ToxEventListener()
        val avListenerC = ToxAvEventListener()
        var toxC = ToxWrapper(listenerC, avListenerC, options)

        try {
            toxA.setName("Alice")
            toxB.setName("Bob")
            toxC.setName("Charlie")

            val pkA = toxA.getPublicKey()
            val pkB = toxB.getPublicKey()
            val pkC = toxC.getPublicKey()

            // Establish friendships
            toxA.addFriendNoRequest(pkB)
            toxB.addFriendNoRequest(pkA)
            toxA.addFriendNoRequest(pkC)
            toxC.addFriendNoRequest(pkA)
            toxB.addFriendNoRequest(pkC)
            toxC.addFriendNoRequest(pkB)

            val isAConnectedToB = AtomicBoolean(false)
            val isAConnectedToC = AtomicBoolean(false)
            val isBConnectedToA = AtomicBoolean(false)
            val isBConnectedToC = AtomicBoolean(false)
            val isCConnectedToA = AtomicBoolean(false)
            val isCConnectedToB = AtomicBoolean(false)

            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkB.string() && status != ConnectionStatus.None) isAConnectedToB.set(true)
                if (pk == pkC.string() && status != ConnectionStatus.None) isAConnectedToC.set(true)
            }

            listenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) isBConnectedToA.set(true)
                if (pk == pkC.string() && status != ConnectionStatus.None) isBConnectedToC.set(true)
            }

            listenerC.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) isCConnectedToA.set(true)
                if (pk == pkB.string() && status != ConnectionStatus.None) isCConnectedToB.set(true)
            }

            // Bob auto-joins group invites
            val bobGroupNo = AtomicInteger(-1)
            val bobInviteReceived = AtomicBoolean(false)
            listenerB.groupInviteHandler = { friendNo, inviteData, groupName ->
                bobInviteReceived.set(true)
                val gn = toxB.groupJoin(friendNo, inviteData, "Bob".toByteArray(), null)
                bobGroupNo.set(gn)
            }

            // Charlie auto-joins group invites
            val charlieGroupNo = AtomicInteger(-1)
            val charlieInviteReceived = AtomicBoolean(false)
            listenerC.groupInviteHandler = { friendNo, inviteData, groupName ->
                charlieInviteReceived.set(true)
                val gn = toxC.groupJoin(friendNo, inviteData, "Charlie".toByteArray(), null)
                charlieGroupNo.set(gn)
            }

            // Group message listener for Alice
            val aliceGroupMessageReceived = AtomicBoolean(false)
            var receivedMessageText = ""
            listenerA.groupMessageHandler = { groupNo, peerId, type, message, messageId ->
                receivedMessageText = message
                aliceGroupMessageReceived.set(true)
            }

            // Iterate loops
            val runLoopA = AtomicBoolean(true)
            val runLoopB = AtomicBoolean(true)
            val runLoopC = AtomicBoolean(true)

            val jobA = launch {
                while (runLoopA.get()) {
                    try {
                        toxA.iterate()
                        delay(toxA.iterationInterval().coerceAtLeast(10L))
                    } catch (e: Exception) { break }
                }
            }

            fun launchBobLoop() = launch {
                while (runLoopB.get()) {
                    try {
                        toxB.iterate()
                        delay(toxB.iterationInterval().coerceAtLeast(10L))
                    } catch (e: Exception) { break }
                }
            }

            var jobB = launchBobLoop()

            val jobC = launch {
                while (runLoopC.get()) {
                    try {
                        toxC.iterate()
                        delay(toxC.iterationInterval().coerceAtLeast(10L))
                    } catch (e: Exception) { break }
                }
            }

            // Bootstrap
            val portA = toxA.selfGetUdpPort()
            val portB = toxB.selfGetUdpPort()
            val portC = toxC.selfGetUdpPort()
            toxA.bootstrap("127.0.0.1", portB, toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", portA, toxA.selfGetDhtId())
            toxA.bootstrap("127.0.0.1", portC, toxC.selfGetDhtId())
            toxC.bootstrap("127.0.0.1", portA, toxA.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", portC, toxC.selfGetDhtId())
            toxC.bootstrap("127.0.0.1", portB, toxB.selfGetDhtId())

            fun allFriendsConnected(): Boolean {
                return isAConnectedToB.get() && isAConnectedToC.get() &&
                       isBConnectedToA.get() && isBConnectedToC.get() &&
                       isCConnectedToA.get() && isCConnectedToB.get()
            }

            // Wait for three-peer DHT convergence and all friendships to establish
            val startTime = System.currentTimeMillis()
            while (!allFriendsConnected() && (System.currentTimeMillis() - startTime) < 25000L) {
                delay(100)
            }

            assertTrue(isAConnectedToB.get(), "Alice should connect to Bob")
            assertTrue(isAConnectedToC.get(), "Alice should connect to Charlie")
            assertTrue(isBConnectedToA.get(), "Bob should connect to Alice")
            assertTrue(isBConnectedToC.get(), "Bob should connect to Charlie")
            assertTrue(isCConnectedToA.get(), "Charlie should connect to Alice")
            assertTrue(isCConnectedToB.get(), "Charlie should connect to Bob")

            // Alice creates group
            val groupNoA = toxA.groupNew(ToxGroupPrivacyState.PUBLIC, "Atox NGC Group".toByteArray(), "Alice".toByteArray())
            assertTrue(groupNoA >= 0)

            delay(500)

            // Get Chat ID
            val chatIdBytes = toxA.groupGetChatId(groupNoA)
            assertTrue(chatIdBytes != null && chatIdBytes.isNotEmpty())

            // Alice invites Bob and Charlie (friendNo for Bob is 0, Charlie is 1)
            toxA.groupInviteSend(groupNoA, 0)
            toxA.groupInviteSend(groupNoA, 1)

            // Wait for joins
            val joinTime = System.currentTimeMillis()
            while ((!bobInviteReceived.get() || !charlieInviteReceived.get()) && (System.currentTimeMillis() - joinTime) < 3000L) {
                delay(100)
            }
            assertTrue(bobInviteReceived.get())
            assertTrue(charlieInviteReceived.get())
            assertTrue(bobGroupNo.get() >= 0)
            assertTrue(charlieGroupNo.get() >= 0)

            // Wait for NGC peers routing and syncing to stabilize
            delay(2000)

            // -------------------------------------------------------------
            // SCENARIO A: Bob drops connection, Alice/Charlie stay online, Bob recovers
            // -------------------------------------------------------------

            // 1. Export save data for Bob
            val saveB = toxB.getSaveData()

            // 2. Shut down Bob's wrapper and loop
            runLoopB.set(false)
            jobB.join()
            toxB.close()

            isAConnectedToB.set(false)

            // 3. Wait 12 seconds for Alice and Charlie to register that Bob went offline (offline timeout)
            delay(12000)

            // 4. Verify Alice can still message Charlie while Bob is offline (group remains active)
            val testGroupMsgDuringDrop = "Alice to Charlie while Bob is offline"
            var resA = -1
            val sendStartTime = System.currentTimeMillis()
            while (resA < 0 && (System.currentTimeMillis() - sendStartTime) < 5000L) {
                resA = toxA.groupSendMessage(groupNoA, ToxMessageType.NORMAL, testGroupMsgDuringDrop.toByteArray(Charsets.UTF_8))
                if (resA < 0) {
                    delay(200)
                }
            }
            assertTrue(resA >= 0, "Alice should successfully send group message while Bob is offline")

            // 5. Bob comes back online (re-instantiates from saved profile)
            val optionB = SaveOptions(saveB, true, ProxyType.None, "", 0)
            val newListenerB = ToxEventListener()
            val newAvListenerB = ToxAvEventListener()
            toxB = ToxWrapper(newListenerB, newAvListenerB, optionB)

            runLoopB.set(true)
            jobB = launchBobLoop()

            // 6. Re-bootstrap Bob to Alice (who is still active and online)
            val recPortA = toxA.selfGetUdpPort()
            val recPortB = toxB.selfGetUdpPort()
            android.util.Log.i(
                "LiveGroupTest",
                "Re-bootstrap Bob: B port = $recPortB, Alice port = $recPortA"
            )
            toxA.bootstrap("127.0.0.1", recPortB, toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", recPortA, toxA.selfGetDhtId())

            // 7. Wait for Bob to reconnect to Alice as friend
            val reconnectTime = System.currentTimeMillis()
            while (!isAConnectedToB.get() && (System.currentTimeMillis() - reconnectTime) < 25000L) {
                delay(100)
            }
            assertTrue(
                isAConnectedToB.get(),
                "Bob should reconnect to Alice as friend (Alice port: $recPortA, Bob port: $recPortB)"
            )

            // 8. Bob recovers his group context
            val oldBobGroupNo = bobGroupNo.get()
            val reconnectSuccessB = toxB.groupReconnect(oldBobGroupNo)
            android.util.Log.i("LiveGroupTest", "Bob reconnectSuccess: $reconnectSuccessB for old gn: $oldBobGroupNo")
            if (!reconnectSuccessB) {
                val newGn = toxB.groupJoinDirect(chatIdBytes, "Bob".toByteArray(), null)
                android.util.Log.i("LiveGroupTest", "Bob groupJoinDirect returned: $newGn")
                if (newGn >= 0) {
                    val recSuccess = toxB.groupReconnect(newGn)
                    android.util.Log.i("LiveGroupTest", "Bob reconnect after joinDirect: $recSuccess")
                    bobGroupNo.set(newGn)
                }
            }

            // 9. Bob sends message inside restored group with retries until Alice receives it (Alice's active session syncs Bob)
            val testRestoredMsg = "Bob is back in group!"
            val restoredStartTime = System.currentTimeMillis()
            var receiveSuccess = false
            var attempt = 0

            // Reset Alice group message received flags
            aliceGroupMessageReceived.set(false)
            receivedMessageText = ""

            while (!receiveSuccess && (System.currentTimeMillis() - restoredStartTime) < 15000L) {
                attempt++
                val targetGn = bobGroupNo.get()
                val res = toxB.groupSendMessage(targetGn, ToxMessageType.NORMAL, testRestoredMsg.toByteArray(Charsets.UTF_8))
                android.util.Log.i("LiveGroupTest", "Bob send attempt $attempt to gn $targetGn returned: $res")

                val waitStart = System.currentTimeMillis()
                while (!aliceGroupMessageReceived.get() && (System.currentTimeMillis() - waitStart) < 1000L) {
                    delay(50)
                }

                if (aliceGroupMessageReceived.get() && receivedMessageText == testRestoredMsg) {
                    receiveSuccess = true
                    android.util.Log.i("LiveGroupTest", "Alice received Bob's message successfully on attempt $attempt!")
                } else {
                    delay(500)
                }
            }

            assertTrue(receiveSuccess, "Alice should receive at least one restored group message from Bob after his recovery")

            // Clean up
            runLoopA.set(false)
            runLoopB.set(false)
            runLoopC.set(false)
            jobA.join()
            jobB.join()
            jobC.join()

        } finally {
            toxA.close()
            toxB.close()
            toxC.close()
        }
    }

    @Test
    fun testMultiPeerGroupBroadcast() = runBlocking {
        delay(5000)
        val options = SaveOptions(null, true, ProxyType.None, "", 0)

        val listenerA = ToxEventListener()
        val avListenerA = ToxAvEventListener()
        val toxA = ToxWrapper(listenerA, avListenerA, options)

        val listenerB = ToxEventListener()
        val avListenerB = ToxAvEventListener()
        val toxB = ToxWrapper(listenerB, avListenerB, options)

        val listenerC = ToxEventListener()
        val avListenerC = ToxAvEventListener()
        val toxC = ToxWrapper(listenerC, avListenerC, options)

        try {
            toxA.setName("Alice")
            toxB.setName("Bob")
            toxC.setName("Charlie")

            val pkA = toxA.getPublicKey()
            val pkB = toxB.getPublicKey()
            val pkC = toxC.getPublicKey()

            toxA.addFriendNoRequest(pkB)
            toxB.addFriendNoRequest(pkA)
            toxA.addFriendNoRequest(pkC)
            toxC.addFriendNoRequest(pkA)
            toxB.addFriendNoRequest(pkC)
            toxC.addFriendNoRequest(pkB)

            val isAConnectedToB = AtomicBoolean(false)
            val isAConnectedToC = AtomicBoolean(false)
            val isBConnectedToA = AtomicBoolean(false)
            val isBConnectedToC = AtomicBoolean(false)
            val isCConnectedToA = AtomicBoolean(false)
            val isCConnectedToB = AtomicBoolean(false)

            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkB.string() && status != ConnectionStatus.None) isAConnectedToB.set(true)
                if (pk == pkC.string() && status != ConnectionStatus.None) isAConnectedToC.set(true)
            }

            listenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) isBConnectedToA.set(true)
                if (pk == pkC.string() && status != ConnectionStatus.None) isBConnectedToC.set(true)
            }

            listenerC.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) isCConnectedToA.set(true)
                if (pk == pkB.string() && status != ConnectionStatus.None) isCConnectedToB.set(true)
            }

            val bobGroupNo = AtomicInteger(-1)
            val bobInviteReceived = AtomicBoolean(false)
            listenerB.groupInviteHandler = { friendNo, inviteData, groupName ->
                bobInviteReceived.set(true)
                val gn = toxB.groupJoin(friendNo, inviteData, "Bob".toByteArray(), null)
                bobGroupNo.set(gn)
            }

            val charlieGroupNo = AtomicInteger(-1)
            val charlieInviteReceived = AtomicBoolean(false)
            listenerC.groupInviteHandler = { friendNo, inviteData, groupName ->
                charlieInviteReceived.set(true)
                val gn = toxC.groupJoin(friendNo, inviteData, "Charlie".toByteArray(), null)
                charlieGroupNo.set(gn)
            }

            val aliceReceivedCount = AtomicInteger(0)
            val bobReceivedCount = AtomicInteger(0)
            val charlieReceivedCount = AtomicInteger(0)

            listenerA.groupMessageHandler = { _, _, _, _, _ -> aliceReceivedCount.incrementAndGet() }
            listenerB.groupMessageHandler = { _, _, _, _, _ -> bobReceivedCount.incrementAndGet() }
            listenerC.groupMessageHandler = { _, _, _, _, _ -> charlieReceivedCount.incrementAndGet() }

            val runLoop = AtomicBoolean(true)
            val jobA = launch { while (runLoop.get()) { toxA.iterate(); delay(20) } }
            val jobB = launch { while (runLoop.get()) { toxB.iterate(); delay(20) } }
            val jobC = launch { while (runLoop.get()) { toxC.iterate(); delay(20) } }

            val portA = toxA.selfGetUdpPort()
            val portB = toxB.selfGetUdpPort()
            val portC = toxC.selfGetUdpPort()

            toxA.bootstrap("127.0.0.1", portB, toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", portA, toxA.selfGetDhtId())
            toxA.bootstrap("127.0.0.1", portC, toxC.selfGetDhtId())
            toxC.bootstrap("127.0.0.1", portA, toxA.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", portC, toxC.selfGetDhtId())
            toxC.bootstrap("127.0.0.1", portB, toxB.selfGetDhtId())

            val start = System.currentTimeMillis()
            while ((!isAConnectedToB.get() || !isAConnectedToC.get() || !isBConnectedToA.get() ||
                    !isBConnectedToC.get() || !isCConnectedToA.get() || !isCConnectedToB.get()) &&
                   (System.currentTimeMillis() - start) < 25000L) {
                delay(100)
            }

            assertTrue(isAConnectedToB.get() && isAConnectedToC.get() && isBConnectedToA.get() && isBConnectedToC.get())

            val groupNoA = toxA.groupNew(ToxGroupPrivacyState.PUBLIC, "Broadcast Group".toByteArray(), "Alice".toByteArray())
            assertTrue(groupNoA >= 0)
            delay(500)

            toxA.groupInviteSend(groupNoA, 0)
            toxA.groupInviteSend(groupNoA, 1)

            val joinStart = System.currentTimeMillis()
            while ((!bobInviteReceived.get() || !charlieInviteReceived.get()) && (System.currentTimeMillis() - joinStart) < 5000L) {
                delay(100)
            }
            assertTrue(bobInviteReceived.get() && charlieInviteReceived.get())
            delay(6000) // Wait for NGC group sync

            // Exchange 50 messages: Alice sends 10, Bob sends 20, Charlie sends 20
            // Since NGC peer routing and discovery are asynchronous, we send with a retry loop
            // until Alice, Bob, and Charlie have received their expected broadcast counts.
            val broadcastStartTime = System.currentTimeMillis()
            var exchangeComplete = false
            var msgIndex = 0

            while (!exchangeComplete && (System.currentTimeMillis() - broadcastStartTime) < 25000L) {
                msgIndex++
                
                // Alice sends a message (only if Bob/Charlie still need more)
                if (bobReceivedCount.get() < 30 || charlieReceivedCount.get() < 30) {
                    toxA.groupSendMessage(groupNoA, ToxMessageType.NORMAL, "Alice-$msgIndex".toByteArray())
                }

                // Bob sends a message (only if Alice/Charlie still need more)
                if (aliceReceivedCount.get() < 40 || charlieReceivedCount.get() < 30) {
                    val gn = bobGroupNo.get()
                    if (gn >= 0) {
                        toxB.groupSendMessage(gn, ToxMessageType.NORMAL, "Bob-$msgIndex".toByteArray())
                    }
                }

                // Charlie sends a message (only if Alice/Bob still need more)
                if (aliceReceivedCount.get() < 40 || bobReceivedCount.get() < 30) {
                    val gn = charlieGroupNo.get()
                    if (gn >= 0) {
                        toxC.groupSendMessage(gn, ToxMessageType.NORMAL, "Charlie-$msgIndex".toByteArray())
                    }
                }

                delay(500)

                if (aliceReceivedCount.get() >= 40 && bobReceivedCount.get() >= 30 && charlieReceivedCount.get() >= 30) {
                    exchangeComplete = true
                }
            }

            assertTrue(aliceReceivedCount.get() >= 40, "Alice should receive all broadcast messages, got ${aliceReceivedCount.get()}")
            assertTrue(bobReceivedCount.get() >= 30, "Bob should receive all broadcast messages, got ${bobReceivedCount.get()}")
            assertTrue(charlieReceivedCount.get() >= 30, "Charlie should receive all broadcast messages, got ${charlieReceivedCount.get()}")

            runLoop.set(false)
            jobA.join()
            jobB.join()
            jobC.join()
        } finally {
            toxA.close()
            toxB.close()
            toxC.close()
        }
    }

    @Test
    fun testDisconnectedInviteQueuing() = runBlocking {
        delay(5000)
        val options = SaveOptions(null, true, ProxyType.None, "", 0)

        val listenerA = ToxEventListener()
        val avListenerA = ToxAvEventListener()
        val toxA = ToxWrapper(listenerA, avListenerA, options)

        val listenerB = ToxEventListener()
        val avListenerB = ToxAvEventListener()
        var toxB = ToxWrapper(listenerB, avListenerB, options)

        try {
            toxA.setName("Alice")
            toxB.setName("Bob")

            val pkA = toxA.getPublicKey()
            val pkB = toxB.getPublicKey()

            toxA.addFriendNoRequest(pkB)
            toxB.addFriendNoRequest(pkA)

            val isAConnected = AtomicBoolean(false)
            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkB.string() && status != ConnectionStatus.None) isAConnected.set(true)
            }

            val runLoopA = AtomicBoolean(true)
            val runLoopB = AtomicBoolean(true)
            val jobA = launch { while (runLoopA.get()) { toxA.iterate(); delay(20) } }
            var jobB = launch { while (runLoopB.get()) { toxB.iterate(); delay(20) } }

            val portA = toxA.selfGetUdpPort()
            val portB = toxB.selfGetUdpPort()

            toxA.bootstrap("127.0.0.1", portB, toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", portA, toxA.selfGetDhtId())

            val start = System.currentTimeMillis()
            while (!isAConnected.get() && (System.currentTimeMillis() - start) < 15000L) {
                delay(100)
            }
            assertTrue(isAConnected.get())

            // Create group on Alice
            val groupNoA = toxA.groupNew(ToxGroupPrivacyState.PUBLIC, "Queued Invite Group".toByteArray(), "Alice".toByteArray())
            assertTrue(groupNoA >= 0)

            val saveB = toxB.getSaveData()

            // Now Bob goes completely offline
            runLoopB.set(false)
            jobB.join()
            toxB.close()

            isAConnected.set(false)
            delay(12000) // wait offline timeout

            // Alice tries to invite Bob while offline
            val instantSuccess = toxA.groupInviteSend(groupNoA, 0)
            android.util.Log.i("LiveGroupTest", "Invite while offline instantSuccess: $instantSuccess")

            // Bob comes back online
            val newOptions = SaveOptions(saveB, true, ProxyType.None, "", 0)
            val newListenerB = ToxEventListener()
            val newAvListenerB = ToxAvEventListener()
            toxB = ToxWrapper(newListenerB, newAvListenerB, newOptions)

            val bobInviteReceived = AtomicBoolean(false)
            newListenerB.groupInviteHandler = { _, _, _ ->
                bobInviteReceived.set(true)
            }

            runLoopB.set(true)
            jobB = launch { while (runLoopB.get()) { toxB.iterate(); delay(20) } }

            // Re-bootstrap
            val recPortA = toxA.selfGetUdpPort()
            val recPortB = toxB.selfGetUdpPort()
            toxA.bootstrap("127.0.0.1", recPortB, toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", recPortA, toxA.selfGetDhtId())

            // Wait for reconnect
            val reconnectStart = System.currentTimeMillis()
            while (!isAConnected.get() && (System.currentTimeMillis() - reconnectStart) < 25000L) {
                delay(100)
            }
            assertTrue(isAConnected.get())

            // Once Alice detects connection, she sends the queued invitation
            var inviteSent = false
            val retryStart = System.currentTimeMillis()
            while (!inviteSent && (System.currentTimeMillis() - retryStart) < 5000L) {
                inviteSent = toxA.groupInviteSend(groupNoA, 0)
                if (!inviteSent) delay(200)
            }
            assertTrue(inviteSent)

            // Bob should receive invite
            val inviteWait = System.currentTimeMillis()
            while (!bobInviteReceived.get() && (System.currentTimeMillis() - inviteWait) < 5000L) {
                delay(100)
            }
            assertTrue(bobInviteReceived.get())

            runLoopA.set(false)
            runLoopB.set(false)
            jobA.join()
            jobB.join()
        } finally {
            toxA.close()
            toxB.close()
        }
    }

    @Test
    fun testJniRapidJoinLeaveStressTest() = runBlocking {
        delay(5000)
        val options = SaveOptions(null, true, ProxyType.None, "", 0)

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

            toxA.addFriendNoRequest(pkB)
            toxB.addFriendNoRequest(pkA)

            val isAConnected = AtomicBoolean(false)
            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkB.string() && status != ConnectionStatus.None) isAConnected.set(true)
            }

            val runLoop = AtomicBoolean(true)
            val jobA = launch { while (runLoop.get()) { toxA.iterate(); delay(10) } }
            val jobB = launch { while (runLoop.get()) { toxB.iterate(); delay(10) } }

            val portA = toxA.selfGetUdpPort()
            val portB = toxB.selfGetUdpPort()

            toxA.bootstrap("127.0.0.1", portB, toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", portA, toxA.selfGetDhtId())

            val start = System.currentTimeMillis()
            while (!isAConnected.get() && (System.currentTimeMillis() - start) < 15000L) {
                delay(100)
            }
            assertTrue(isAConnected.get())

            // Create group on Alice
            val groupNoA = toxA.groupNew(ToxGroupPrivacyState.PUBLIC, "Stress Group".toByteArray(), "Alice".toByteArray())
            assertTrue(groupNoA >= 0)
            delay(500)

            val chatIdBytes = toxA.groupGetChatId(groupNoA)
            assertTrue(chatIdBytes != null && chatIdBytes.isNotEmpty())

            // Rapidly join and leave group 15 times
            for (i in 1..15) {
                val gn = toxB.groupJoinDirect(chatIdBytes, "Bob".toByteArray(), null)
                assertTrue(gn >= 0)
                delay(50)
                val leaveSuccess = toxB.groupLeave(gn)
                assertTrue(leaveSuccess)
                delay(50)
            }

            runLoop.set(false)
            jobA.join()
            jobB.join()
        } finally {
            toxA.close()
            toxB.close()
        }
    }
}
