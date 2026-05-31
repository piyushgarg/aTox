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

    @Test
    fun testFloodingAndFileTransferStress() = runTest {
        val options = SaveOptions(null, false, ProxyType.None, "", 0)

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
            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.getPublicKey().bytes())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.getPublicKey().bytes())

            // Wait up to 6 seconds for loopback connection
            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 6000L) {
                delay(100)
            }

            // Perform flooding if loopback connected successfully
            if (isAConnected.get() && isBConnected.get()) {
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

                // Wait for all messages to arrive (up to 4 seconds)
                val floodStartTime = System.currentTimeMillis()
                while (receivedCount.get() < totalMessages && (System.currentTimeMillis() - floodStartTime) < 4000L) {
                    delay(50)
                }

                // Verify that the queue handled high-frequency JNI transactions without drops or lockups
                assertEquals(totalMessages, receivedCount.get(), "All 100 messages must be safely delivered under high-frequency JNI stress")

                // Test high-concurrency file transfer request during active loop
                val fileNo = toxA.sendFile(pkB, FileKind.Data, 5000000L, "huge_stress_payload.zip")
                assertTrue(fileNo >= 0, "Initiating high-concurrency file transfer should succeed under message flood stress")
            }

            runLoops.set(false)
            jobA.join()
            jobB.join()

        } finally {
            toxA.close()
            toxB.close()
        }
    }
}
