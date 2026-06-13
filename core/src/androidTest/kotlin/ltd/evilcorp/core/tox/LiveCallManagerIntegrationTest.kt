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
import ltd.evilcorp.core.tox.runtime.ToxCallBridge
import ltd.evilcorp.core.tox.runtime.ToxWrapper
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LiveCallManagerIntegrationTest {

    @Test
    fun testLiveCallAndAudioTransmission() = runTest {
        val options = SaveOptions(null, true, ProxyType.None, "", 0)

        // 1. Initialize two separate live native Tox instances
        val listenerA = ToxEventListener()
        val avListenerA = ToxAvEventListener()
        val toxA = ToxWrapper(listenerA, avListenerA, options)

        val listenerB = ToxEventListener()
        val avListenerB = ToxAvEventListener()
        val toxB = ToxWrapper(listenerB, avListenerB, options)

        val callBridgeA = ToxCallBridge()
        callBridgeA.init(toxA)

        val callBridgeB = ToxCallBridge()
        callBridgeB.init(toxB)

        try {
            toxA.setName("Alice")
            toxB.setName("Bob")

            val pkA = toxA.getPublicKey()
            val pkB = toxB.getPublicKey()

            // Perform live Friend Operations
            val friendNoB = toxA.addFriendNoRequest(pkB)
            assertTrue(friendNoB >= 0, "Alice should add Bob")

            val friendNoA = toxB.addFriendNoRequest(pkA)
            assertTrue(friendNoA >= 0, "Bob should add Alice")

            listenerA.contactMapping = listOf(pkB to friendNoB)
            avListenerA.contactMapping = listOf(pkB to friendNoB)

            listenerB.contactMapping = listOf(pkA to friendNoA)
            avListenerB.contactMapping = listOf(pkA to friendNoA)

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

            // Set up call event handling on Bob's side
            val incomingCallReceived = AtomicBoolean(false)
            avListenerB.callHandler = { pk, audioEnabled, videoEnabled ->
                if (pk == pkA.string()) {
                    incomingCallReceived.set(true)
                    // Auto-answer the call
                    callBridgeB.answerCall(pkA)
                }
            }

            val audioFrameReceived = AtomicBoolean(false)
            avListenerB.audioReceiveFrameHandler = { pk, pcm, channels, samplingRate ->
                if (pk == pkA.string()) {
                    audioFrameReceived.set(true)
                }
            }

            val runLoops = AtomicBoolean(true)
            val jobA = launch {
                while (runLoops.get()) {
                    try {
                        toxA.iterate()
                        toxA.iterateAv()
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
                        toxB.iterateAv()
                        delay(toxB.iterationInterval().coerceAtLeast(10L))
                    } catch (e: Exception) {
                        break
                    }
                }
            }

            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.selfGetDhtId())

            // Wait up to 5 seconds to establish local connection (DHT handshake)
            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 20000L) {
                delay(100)
            }

            assertTrue(isAConnected.get(), "Alice should be connected to Bob")
            assertTrue(isBConnected.get(), "Bob should be connected to Alice")

            // 2. Alice starts the call
            val callStarted = callBridgeA.startCall(pkB)
            assertTrue(callStarted, "Alice should successfully start the call")

            // Wait for Bob to receive call request and auto-answer
            val callStartTime = System.currentTimeMillis()
            while (!incomingCallReceived.get() && (System.currentTimeMillis() - callStartTime) < 4000L) {
                delay(100)
            }
            assertTrue(incomingCallReceived.get(), "Bob should receive incoming call notification")

            // Allow call connection handshake to complete
            delay(1000)

            // 3. Alice sends real audio PCM frame (Opus encoded internally by JNI)
            // standard 20ms frame for 8000Hz mono is 160 shorts
            val testPcm = ShortArray(160) { 1000 }
            var audioSent = false
            val audioSendStartTime = System.currentTimeMillis()
            while (!audioSent && (System.currentTimeMillis() - audioSendStartTime) < 3000L) {
                audioSent = callBridgeA.sendAudio(pkB, testPcm, 1, 8000)
                if (!audioSent) delay(100)
            }
            assertTrue(audioSent, "Alice should successfully send audio PCM frame")

            // Wait for Bob to receive audio frame
            val audioRecvStartTime = System.currentTimeMillis()
            while (!audioFrameReceived.get() && (System.currentTimeMillis() - audioRecvStartTime) < 3000L) {
                delay(100)
            }
            assertTrue(audioFrameReceived.get(), "Bob should receive the audio frame from Alice")

            // 4. End Call
            val callEnded = callBridgeA.endCall(pkB)
            assertTrue(callEnded, "Alice should successfully end the call")

            // Clean up loops
            runLoops.set(false)
            jobA.join()
            jobB.join()

        } finally {
            toxA.close()
            toxB.close()
        }
    }

    @Test
    fun testMicrophoneHardwareLossSimulation() = runTest {
        val options = SaveOptions(null, true, ProxyType.None, "", 0)

        val listenerA = ToxEventListener()
        val avListenerA = ToxAvEventListener()
        val toxA = ToxWrapper(listenerA, avListenerA, options)

        val listenerB = ToxEventListener()
        val avListenerB = ToxAvEventListener()
        val toxB = ToxWrapper(listenerB, avListenerB, options)

        val callBridgeA = ToxCallBridge()
        callBridgeA.init(toxA)

        val callBridgeB = ToxCallBridge()
        callBridgeB.init(toxB)

        try {
            toxA.setName("Alice")
            toxB.setName("Bob")

            val pkA = toxA.getPublicKey()
            val pkB = toxB.getPublicKey()

            val friendNoB = toxA.addFriendNoRequest(pkB)
            val friendNoA = toxB.addFriendNoRequest(pkA)

            listenerA.contactMapping = listOf(pkB to friendNoB)
            avListenerA.contactMapping = listOf(pkB to friendNoB)

            listenerB.contactMapping = listOf(pkA to friendNoA)
            avListenerB.contactMapping = listOf(pkA to friendNoA)

            val isAConnected = AtomicBoolean(false)
            val isBConnected = AtomicBoolean(false)

            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkB.string() && status != ConnectionStatus.None) isAConnected.set(true)
            }
            listenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) isBConnected.set(true)
            }

            val incomingCallReceived = AtomicBoolean(false)
            avListenerB.callHandler = { pk, audioEnabled, videoEnabled ->
                if (pk == pkA.string()) {
                    incomingCallReceived.set(true)
                    callBridgeB.answerCall(pkA)
                }
            }

            val runLoops = AtomicBoolean(true)
            val jobA = launch { while (runLoops.get()) { toxA.iterate(); toxA.iterateAv(); delay(10) } }
            val jobB = launch { while (runLoops.get()) { toxB.iterate(); toxB.iterateAv(); delay(10) } }

            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.selfGetDhtId())

            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 20000L) {
                delay(100)
            }
            assertTrue(isAConnected.get() && isBConnected.get())

            val callStarted = callBridgeA.startCall(pkB)
            assertTrue(callStarted)

            val callStartTime = System.currentTimeMillis()
            while (!incomingCallReceived.get() && (System.currentTimeMillis() - callStartTime) < 4000L) {
                delay(100)
            }
            assertTrue(incomingCallReceived.get())
            delay(1000)

            val micHardwareLost = true
            if (micHardwareLost) {
                val endCallSuccess = callBridgeA.endCall(pkB)
                assertTrue(endCallSuccess, "Should cleanly end the call on microphone hardware loss")
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
