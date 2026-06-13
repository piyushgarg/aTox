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
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.transfer.model.FileKind
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ToxNetworkStressTest {

    @Suppress("LongMethod")
    @Test
    fun testFloodingAndFileTransferStress() = runTest {
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

            val friendNoB = toxA.addFriendNoRequest(pkB)
            assertTrue(friendNoB >= 0)
            val friendNoA = toxB.addFriendNoRequest(pkA)
            assertTrue(friendNoA >= 0)

            val isAConnected = AtomicBoolean(false)
            val isBConnected = AtomicBoolean(false)

            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkB.string() && status != ConnectionStatus.None) {
                    isAConnected.set(true)
                }
            }

            listenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) {
                    isBConnected.set(true)
                }
            }

            // Start active JNI loop iteration
            val runLoops = AtomicBoolean(true)
            val jobA = launch {
                while (runLoops.get()) {
                    try {
                        toxA.iterate()
                        delay(10)
                    } catch (e: Exception) {
                        break
                    }
                }
            }
            val jobB = launch {
                while (runLoops.get()) {
                    try {
                        toxB.iterate()
                        delay(10)
                    } catch (e: Exception) {
                        break
                    }
                }
            }

            // Bootstrap
            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.selfGetDhtId())

            // Wait up to 15 seconds for loopback connection
            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 15000L) {
                delay(100)
            }

            assertTrue(isAConnected.get() && isBConnected.get(), "Alice and Bob must connect")

            val totalMessages = 100
            val receivedCount = AtomicInteger(0)

            listenerB.friendMessageHandler = { pk, type, timeDelta, message ->
                if (pk == pkA.string() && message.startsWith("Stress message")) {
                    receivedCount.incrementAndGet()
                }
            }

            // Alice floods Bob with 100 messages as fast as possible
            for (i in 0 until totalMessages) {
                toxA.sendMessage(pkB, "Stress message $i", MessageType.Normal)
            }

            // Wait for all messages to arrive (up to 6 seconds)
            val floodStartTime = System.currentTimeMillis()
            while (receivedCount.get() < totalMessages && (System.currentTimeMillis() - floodStartTime) < 6000L) {
                delay(50)
            }

            // Verify that the queue handled high-frequency JNI transactions without drops or lockups
            assertEquals(totalMessages, receivedCount.get(), "All 100 messages must be delivered")

            // Test high-concurrency file transfer request during active loop
            val fileNo = toxA.sendFile(pkB, FileKind.Data, 5000000L, "huge_stress_payload.zip")
            assertTrue(fileNo >= 0, "Initiating file transfer should succeed")

            runLoops.set(false)
            jobA.join()
            jobB.join()

        } finally {
            toxA.close()
            toxB.close()
        }
    }

    @Suppress("LongMethod")
    @Test
    fun testFiveHundredMessagesFloodWithResponsesAndInterruption() = runTest {
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

            val friendNoB = toxA.addFriendNoRequest(pkB)
            assertTrue(friendNoB >= 0)
            val friendNoA = toxB.addFriendNoRequest(pkA)
            assertTrue(friendNoA >= 0)

            val isAConnected = AtomicBoolean(false)
            val isBConnected = AtomicBoolean(false)

            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkB.string() && status != ConnectionStatus.None) {
                    isAConnected.set(true)
                }
            }

            listenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) {
                    isBConnected.set(true)
                }
            }

            val runLoopsA = AtomicBoolean(true)
            val runLoopsB = AtomicBoolean(true)

            val jobA = launch {
                while (runLoopsA.get()) {
                    try {
                        toxA.iterate()
                        delay(10)
                    } catch (e: Exception) { break }
                }
            }

            fun launchBobLoop() = launch {
                while (runLoopsB.get()) {
                    try {
                        toxB.iterate()
                        delay(10)
                    } catch (e: Exception) { break }
                }
            }

            var jobB = launchBobLoop()

            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.selfGetDhtId())

            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 15000L) {
                delay(100)
            }

            assertTrue(isAConnected.get() && isBConnected.get(), "Alice and Bob must connect")

            val totalMessages = 500
            val aliceReceivedCount = AtomicInteger(0)
            val bobReceivedCount = AtomicInteger(0)

            // Setup listeners
            listenerB.friendMessageHandler = { pk, type, timeDelta, message ->
                if (pk == pkA.string() && message.startsWith("Msg ")) {
                    val numStr = message.substringAfter("Msg ")
                    bobReceivedCount.incrementAndGet()
                    // Bob replies immediately back to Alice with Ack
                    toxB.sendMessage(pkA, "Ack $numStr", MessageType.Normal)
                }
            }

            listenerA.friendMessageHandler = { pk, type, timeDelta, message ->
                if (pk == pkB.string() && message.startsWith("Ack ")) {
                    aliceReceivedCount.incrementAndGet()
                }
            }

            // 1. Send first batch of 250 messages
            for (i in 0 until 250) {
                toxA.sendMessage(pkB, "Msg $i", MessageType.Normal)
            }

            // Wait a moment for Bob to receive most of them
            delay(500)

            // 2. Simulate interruption: Freeze Bob's event loop
            runLoopsB.set(false)
            jobB.join()

            // 3. While Bob's loop is frozen, Alice continues sending the next 250 messages
            for (i in 250 until totalMessages) {
                toxA.sendMessage(pkB, "Msg $i", MessageType.Normal)
            }

            // Wait 1 second while Bob is frozen to accumulate backlog
            delay(1000)

            // 4. Bob wakes up (resume Bob's event loop)
            runLoopsB.set(true)
            jobB = launchBobLoop()

            // 5. Wait for all 500 messages and 500 responses to be delivered (up to 15 seconds)
            val floodStartTime = System.currentTimeMillis()
            while ((aliceReceivedCount.get() < totalMessages || bobReceivedCount.get() < totalMessages) &&
                (System.currentTimeMillis() - floodStartTime) < 15000L
            ) {
                delay(100)
            }

            // Verify that no messages were dropped despite loop freeze and high volume
            assertEquals(
                totalMessages,
                bobReceivedCount.get(),
                "Bob must receive all 500 messages despite freeze"
            )
            assertEquals(
                totalMessages,
                aliceReceivedCount.get(),
                "Alice must receive all 500 responses despite B's temporary freeze"
            )

            runLoopsA.set(false)
            runLoopsB.set(false)
            jobA.join()
            jobB.join()

        } finally {
            toxA.close()
            toxB.close()
        }
    }
}
