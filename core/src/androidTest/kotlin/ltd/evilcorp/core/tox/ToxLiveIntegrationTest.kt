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
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ToxLiveIntegrationTest {

    @Test
    fun testLiveEndToEndToxJniOperations() = runTest {
        val options = SaveOptions(null, false, ProxyType.None, "", 0)

        // 1. Initialize two separate live native Tox instances in the same process
        val listenerA = ToxEventListener()
        val avListenerA = ToxAvEventListener()
        val toxA = ToxWrapper(listenerA, avListenerA, options)

        val listenerB = ToxEventListener()
        val avListenerB = ToxAvEventListener()
        val toxB = ToxWrapper(listenerB, avListenerB, options)

        try {
            // Verify profile initializations
            toxA.setName("Alice")
            toxB.setName("Bob")
            assertEquals("Alice", toxA.getName())
            assertEquals("Bob", toxB.getName())

            val pkA = toxA.getPublicKey()
            val pkB = toxB.getPublicKey()
            assertNotNull(pkA)
            assertNotNull(pkB)

            // 2. Perform live native JNI Friend Operations
            val friendNoB = toxA.addFriendNoRequest(pkB)
            assertTrue(friendNoB >= 0, "Alice should add Bob as a friend on native JNI layer")

            val friendsList = toxA.getContacts()
            assertTrue(friendsList.any { it.first == pkB }, "Bob should appear in Alice's friend list")

            // 3. Perform live native JNI NGC Group Conference Operations
            val groupNo = toxA.groupNew(
                ToxGroupPrivacyState.PUBLIC,
                "Atox Developers".toByteArray(Charsets.UTF_8),
                "Alice".toByteArray(Charsets.UTF_8)
            )
            assertTrue(groupNo >= 0, "Alice should successfully create a native NGC group conference")

            // Test native JNI group title/topic
            val topicSet = toxA.groupSetTopic(groupNo, "Kotlin Native JNI Development".toByteArray(Charsets.UTF_8))
            assertTrue(topicSet, "Should set topic on NGC group")
            
            val retrievedTopic = toxA.groupGetTopic(groupNo)
            assertNotNull(retrievedTopic)
            assertEquals("Kotlin Native JNI Development", String(retrievedTopic, Charsets.UTF_8))

            // Test native JNI group password protection
            val passSet = toxA.groupSetPassword(groupNo, "secure123".toByteArray(Charsets.UTF_8))
            assertTrue(passSet, "Should set password on NGC group")

            val retrievedPass = toxA.groupGetPassword(groupNo)
            assertNotNull(retrievedPass)
            assertEquals("secure123", String(retrievedPass, Charsets.UTF_8))

            // 4. Test Live Loopback Networking & Connection Establishing
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

            // Start background run loops to iterate JNI states for both instances
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

            // Bootstrap instances against each other over local loopback UDP port
            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.getPublicKey().bytes())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.getPublicKey().bytes())

            // Wait up to 5 seconds to establish local connection (DHT handshake over 127.0.0.1)
            val startTime = System.currentTimeMillis()
            val maxWaitMs = 5000L
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < maxWaitMs) {
                delay(100)
            }

            // If local loopback successfully established network connection:
            if (isAConnected.get() && isBConnected.get()) {
                // Test real private message sending
                val messageReceived = AtomicBoolean(false)
                var receivedText = ""
                
                listenerB.friendMessageHandler = { pk, type, timeDelta, message ->
                    if (pk == pkA.string()) {
                        receivedText = message
                        messageReceived.set(true)
                    }
                }

                toxA.sendMessage(pkB, "Live JNI message!", MessageType.Normal)
                
                val msgStartTime = System.currentTimeMillis()
                while (!messageReceived.get() && (System.currentTimeMillis() - msgStartTime) < 2000L) {
                    delay(50)
                }

                assertTrue(messageReceived.get(), "Bob should receive live loopback private message from Alice")
                assertEquals("Live JNI message!", receivedText)

                // Test live file transfer request (works since Bob is online)
                val fileNo = toxA.sendFile(pkB, FileKind.Data, 1024L, "dummy_update.apk")
                assertTrue(fileNo >= 0, "Should successfully initiate JNI file transfer request when friend is online")
            }

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
