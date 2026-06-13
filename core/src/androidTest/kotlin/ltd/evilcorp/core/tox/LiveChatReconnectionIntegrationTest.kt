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
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.chat.model.MessageType
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LiveChatReconnectionIntegrationTest {

    @Suppress("LongMethod")
    @Test
    fun testLiveChatReconnectionAndRecovery() = runBlocking {
        // Allow lingering port resources from previous runs/tests to be fully released by the OS
        delay(2000)

        val options = SaveOptions(null, true, ProxyType.None, "", 0)

        // 1. Init Alice and Bob
        val listenerA = ToxEventListener()
        val avListenerA = ToxAvEventListener()
        var toxA = ToxWrapper(listenerA, avListenerA, options)

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
            val isBConnected = AtomicBoolean(false)

            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkB.string() && status != ConnectionStatus.None) isAConnected.set(true)
            }

            listenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) isBConnected.set(true)
            }

            var bobReceivedMsg = ""
            val bobMsgReceived = AtomicBoolean(false)
            listenerB.friendMessageHandler = { pk, type, timeDelta, message ->
                bobReceivedMsg = message
                bobMsgReceived.set(true)
            }

            val runLoopA = AtomicBoolean(true)
            val runLoopB = AtomicBoolean(true)

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

            // Bootstrap
            val portA = toxA.selfGetUdpPort()
            val portB = toxB.selfGetUdpPort()
            android.util.Log.d("LiveChatTest", "Initial Bootstrap - Alice port: $portA, Bob port: $portB")

            toxA.bootstrap("127.0.0.1", portB, toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", portA, toxA.selfGetDhtId())

            // Wait to connect (increased timeout to 15 seconds for emulator environment)
            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 15000L) {
                delay(100)
            }
            assertTrue(isAConnected.get(), "Alice should connect to Bob initially (Alice port: $portA, Bob port: $portB)")

            // Exchange first message
            val testMsg = "Hello Bob!"
            val msgId = toxA.sendMessage(pkB, testMsg, MessageType.Normal)
            assertTrue(msgId >= 0)

            val msgTime = System.currentTimeMillis()
            while (!bobMsgReceived.get() && (System.currentTimeMillis() - msgTime) < 3000L) {
                delay(100)
            }
            assertTrue(bobMsgReceived.get())
            assertEquals(testMsg, bobReceivedMsg)

            // -------------------------------------------------------------
            // SCENARIO: Bob drops connection, Alice queues/sends, Bob reconnects
            // -------------------------------------------------------------

            val saveB = toxB.getSaveData()

            // Stop Bob loop and wrapper
            runLoopB.set(false)
            jobB.join()
            toxB.close()

            isAConnected.set(false)
            isBConnected.set(false)

            // Wait for Alice to detect disconnect (exceed Toxcore's 10-second friend offline timeout)
            delay(12000)

            // Bob comes back online
            val optionB = SaveOptions(saveB, true, ProxyType.None, "", 0)
            val newListenerB = ToxEventListener()
            val newAvListenerB = ToxAvEventListener()
            toxB = ToxWrapper(newListenerB, newAvListenerB, optionB)

            val newBobMsgReceived = AtomicBoolean(false)
            var newReceivedMsg = ""
            newListenerB.friendMessageHandler = { pk, type, timeDelta, message ->
                newReceivedMsg = message
                newBobMsgReceived.set(true)
            }

            newListenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) isBConnected.set(true)
            }

            runLoopB.set(true)
            jobB = launchBobLoop()

            // Re-bootstrap
            val recPortA = toxA.selfGetUdpPort()
            val recPortB = toxB.selfGetUdpPort()
            android.util.Log.d("LiveChatTest", "Re-bootstrap - Alice port: $recPortA, Bob port: $recPortB")

            toxA.bootstrap("127.0.0.1", recPortB, toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", recPortA, toxA.selfGetDhtId())

            // Wait to reconnect (increased timeout to 25 seconds for robust DHT convergence)
            val reconnectTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - reconnectTime) < 25000L) {
                delay(100)
            }
            assertTrue(isAConnected.get(), "Alice should reconnect to Bob after recovery (Alice port: $recPortA, Bob port: $recPortB)")

            // Send message after recovery
            val testMsg2 = "Welcome back Bob!"
            var sendSuccess = false
            val startSendTime = System.currentTimeMillis()
            while (!sendSuccess && (System.currentTimeMillis() - startSendTime) < 3000L) {
                val res = toxA.sendMessage(pkB, testMsg2, MessageType.Normal)
                sendSuccess = res >= 0
                if (!sendSuccess) delay(200)
            }
            assertTrue(sendSuccess)

            val receiveTime = System.currentTimeMillis()
            while (!newBobMsgReceived.get() && (System.currentTimeMillis() - receiveTime) < 3000L) {
                delay(100)
            }
            assertTrue(newBobMsgReceived.get())
            assertEquals(testMsg2, newReceivedMsg)

            // Clean up
            runLoopA.set(false)
            runLoopB.set(false)
            jobA.join()
            jobB.join()

        } finally {
            toxA.close()
            toxB.close()
        }
    }
}
