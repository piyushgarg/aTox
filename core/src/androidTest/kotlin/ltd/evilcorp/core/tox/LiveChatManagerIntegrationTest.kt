// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.repository.ContactRepositoryImpl
import ltd.evilcorp.core.repository.MessageRepositoryImpl
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.runtime.ToxWrapper
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.IToxMessenger
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.settings.model.ProxyType
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LiveChatManagerIntegrationTest {

    private class ToxWrapperMessenger(private val wrapper: ToxWrapper) : IToxMessenger {
        override fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int {
            return wrapper.sendMessage(publicKey, message, type)
        }

        override fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean {
            wrapper.setTyping(publicKey, typing)
            return true
        }

        override fun friendGetTyping(publicKey: PublicKey): Boolean {
            return wrapper.friendGetTyping(publicKey)
        }

        override fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean {
            wrapper.sendLosslessPacket(pk, packet)
            return true
        }
    }

    @Test
    fun testLiveChatManagerEndToEndMessageExchange() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val options = SaveOptions(null, true, ProxyType.None, "", 0)

        // 1. Initialize two separate Databases for Alice and Bob
        val dbAlice = Room.inMemoryDatabaseBuilder(context, Database::class.java).build()
        val dbBob = Room.inMemoryDatabaseBuilder(context, Database::class.java).build()

        val contactRepoAlice = ContactRepositoryImpl(dbAlice.contactDao())
        val messageRepoAlice = MessageRepositoryImpl(dbAlice)

        val contactRepoBob = ContactRepositoryImpl(dbBob.contactDao())
        val messageRepoBob = MessageRepositoryImpl(dbBob)

        // 2. Initialize two live native Tox wrappers
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

            // Pre-populate contact repositories
            contactRepoAlice.add(Contact(pkB.string(), "Bob", connectionStatus = ConnectionStatus.None))
            contactRepoBob.add(Contact(pkA.string(), "Alice", connectionStatus = ConnectionStatus.None))

            // Add friends on native JNI layer
            val friendNoB = toxA.addFriendNoRequest(pkB)
            assertTrue(friendNoB >= 0, "Alice should add Bob")
            val friendNoA = toxB.addFriendNoRequest(pkA)
            assertTrue(friendNoA >= 0, "Bob should add Alice")

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

            // Bootstrap over loopback
            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.selfGetDhtId())

            // Wait up to 5 seconds to establish local connection
            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 20000L) {
                delay(100)
            }

            assertTrue(isAConnected.get(), "Alice should be connected to Bob")
            assertTrue(isBConnected.get(), "Bob should be connected to Alice")

            // Update connection statuses in repositories
            contactRepoAlice.setConnectionStatus(pkB.string(), ConnectionStatus.UDP)
            contactRepoBob.setConnectionStatus(pkA.string(), ConnectionStatus.UDP)

            // Setup real ChatManager for Alice
            val messengerA = ToxWrapperMessenger(toxA)
            val chatManagerAlice = ChatManager(this, contactRepoAlice, messageRepoAlice, messengerA, Dispatchers.Unconfined)

            // Register message handler on Bob to simulate FriendDatabaseUpdater behavior
            val messageReceived = AtomicBoolean(false)
            var receivedText = ""
            listenerB.friendMessageHandler = { pk, type, timeDelta, message ->
                if (pk == pkA.string()) {
                    receivedText = message
                    messageReceived.set(true)
                    launch {
                        messageRepoBob.add(
                            ltd.evilcorp.domain.features.chat.model.Message(
                                pk,
                                message,
                                ltd.evilcorp.domain.features.chat.model.Sender.Received,
                                type.toMessageType(),
                                correlationId = Int.MIN_VALUE
                            )
                        )
                    }
                }
            }

            // 3. Send message via Alice's ChatManager
            val testMsg = "Hello Bob from real ChatManager!"
            chatManagerAlice.sendMessage(pkB, testMsg, MessageType.Normal)

            // Wait for Bob to receive the message over JNI loopback and save to DB
            val msgStartTime = System.currentTimeMillis()
            while (!messageReceived.get() && (System.currentTimeMillis() - msgStartTime) < 3000L) {
                delay(50)
            }

            assertTrue(messageReceived.get(), "Bob should receive the message")
            assertEquals(testMsg, receivedText)

            // 4. Verify DB persistency on Alice's side
            delay(150) // allow async save to complete
            val aliceMessages = messageRepoAlice.get(pkB.string()).first()
            val aliceLastMsg = aliceMessages.firstOrNull()
            assertNotNull(aliceLastMsg)
            assertEquals(testMsg, aliceLastMsg.message)
            assertEquals(ltd.evilcorp.domain.features.chat.model.Sender.Sent, aliceLastMsg.sender)

            // 5. Verify DB persistency on Bob's side
            delay(150) // allow async save to complete
            val bobMessages = messageRepoBob.get(pkA.string()).first()
            val bobLastMsg = bobMessages.firstOrNull()
            assertNotNull(bobLastMsg)
            assertEquals(testMsg, bobLastMsg.message)
            assertEquals(ltd.evilcorp.domain.features.chat.model.Sender.Received, bobLastMsg.sender)

            // Clean up loops
            runLoops.set(false)
            jobA.join()
            jobB.join()

        } finally {
            toxA.close()
            toxB.close()
            dbAlice.close()
            dbBob.close()
        }
    }
}
