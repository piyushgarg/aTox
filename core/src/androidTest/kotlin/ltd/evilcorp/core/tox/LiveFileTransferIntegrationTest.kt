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
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LiveFileTransferIntegrationTest {

    @Test
    fun testLiveFileTransferTransmission() = runTest {
        val options = SaveOptions(null, true, ProxyType.None, "", 0)

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
                if (pk == pkB.string() && status != ConnectionStatus.None) {
                    isAConnected.set(true)
                }
            }

            listenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) {
                    isBConnected.set(true)
                }
            }

            // Generate 15 KB of dummy file data
            val fileSize = 15360
            val fileData = ByteArray(fileSize) { i -> (i % 256).toByte() }

            // Set up File transfer event listeners
            val fileRecvCalled = AtomicBoolean(false)
            val incomingFileNo = AtomicInteger(-1)

            listenerB.fileRecvHandler = { publicKey, fileNo, kind, size, name ->
                if (publicKey == pkA.string()) {
                    incomingFileNo.set(fileNo)
                    fileRecvCalled.set(true)
                    // Accept/resume file transfer from Bob's side
                    toxB.startFileTransfer(pkA, fileNo)
                }
            }

            listenerA.fileChunkRequestHandler = { publicKey, fileNo, position, length ->
                if (publicKey == pkB.string()) {
                    val offset = position.toInt()
                    val end = (offset + length).coerceAtMost(fileSize)
                    if (offset < fileSize) {
                        val chunk = fileData.copyOfRange(offset, end)
                        toxA.sendFileChunk(pkB, fileNo, position, chunk)
                    }
                }
            }

            val receivedBuffer = ByteArray(fileSize)
            val receivedBytes = AtomicInteger(0)
            val fileTransferCompleted = AtomicBoolean(false)

            listenerB.fileRecvChunkHandler = { publicKey, fileNo, position, data ->
                if (publicKey == pkA.string() && fileNo == incomingFileNo.get()) {
                    System.arraycopy(data, 0, receivedBuffer, position.toInt(), data.size)
                    val total = receivedBytes.addAndGet(data.size)
                    if (total >= fileSize) {
                        fileTransferCompleted.set(true)
                    }
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

            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.selfGetDhtId())

            // Wait up to 5 seconds to establish connection
            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 20000L) {
                delay(100)
            }

            assertTrue(isAConnected.get(), "Alice should connect to Bob")
            assertTrue(isBConnected.get(), "Bob should connect to Alice")

            // 2. Alice initiates sending the file
            val fileNo = toxA.sendFile(pkB, FileKind.Data, fileSize.toLong(), "live_test_file.bin")
            assertTrue(fileNo >= 0, "Alice should successfully initiate file transfer")

            // Wait for Bob to receive the transfer request and auto-accept
            val recvTime = System.currentTimeMillis()
            while (!fileRecvCalled.get() && (System.currentTimeMillis() - recvTime) < 3000L) {
                delay(100)
            }
            assertTrue(fileRecvCalled.get(), "Bob should receive incoming file transfer request")

            // Wait for transfer completion
            val transTime = System.currentTimeMillis()
            while (!fileTransferCompleted.get() && (System.currentTimeMillis() - transTime) < 5000L) {
                delay(100)
            }
            assertTrue(fileTransferCompleted.get(), "File transfer should fully complete")

            // 3. Verify integrity of transmitted file data
            for (i in 0 until fileSize) {
                assertTrue(fileData[i] == receivedBuffer[i], "Data mismatch at byte index $i")
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

    @Test
    fun testInsufficientStorageAbort() = runTest {
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
            val isBConnected = AtomicBoolean(false)

            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkB.string() && status != ConnectionStatus.None) isAConnected.set(true)
            }
            listenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) isBConnected.set(true)
            }

            val fileSize = 15360
            val fileData = ByteArray(fileSize) { i -> (i % 256).toByte() }

            val fileRecvCalled = AtomicBoolean(false)
            val incomingFileNo = AtomicInteger(-1)

            listenerB.fileRecvHandler = { publicKey, fileNo, kind, size, name ->
                if (publicKey == pkA.string()) {
                    incomingFileNo.set(fileNo)
                    fileRecvCalled.set(true)
                    toxB.startFileTransfer(pkA, fileNo)
                }
            }

            listenerA.fileChunkRequestHandler = { publicKey, fileNo, position, length ->
                if (publicKey == pkB.string()) {
                    val offset = position.toInt()
                    val end = (offset + length).coerceAtMost(fileSize)
                    if (offset < fileSize) {
                        val chunk = fileData.copyOfRange(offset, end)
                        toxA.sendFileChunk(pkB, fileNo, position, chunk)
                    }
                }
            }

            val fileTransferAborted = AtomicBoolean(false)
            val receivedBytes = AtomicInteger(0)

            listenerB.fileRecvChunkHandler = { publicKey, fileNo, position, data ->
                if (publicKey == pkA.string() && fileNo == incomingFileNo.get()) {
                    val total = receivedBytes.addAndGet(data.size)
                    if (total > 5000) {
                        toxB.stopFileTransfer(pkA, fileNo)
                        fileTransferAborted.set(true)
                    }
                }
            }

            val runLoops = AtomicBoolean(true)
            val jobA = launch { while (runLoops.get()) { toxA.iterate(); delay(10) } }
            val jobB = launch { while (runLoops.get()) { toxB.iterate(); delay(10) } }

            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.selfGetDhtId())

            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 20000L) {
                delay(100)
            }
            assertTrue(isAConnected.get() && isBConnected.get())

            val fileNo = toxA.sendFile(pkB, FileKind.Data, fileSize.toLong(), "abort_test.bin")
            assertTrue(fileNo >= 0)

            val waitTime = System.currentTimeMillis()
            while (!fileTransferAborted.get() && (System.currentTimeMillis() - waitTime) < 5000L) {
                delay(100)
            }
            assertTrue(fileTransferAborted.get(), "File transfer should be aborted successfully")

            runLoops.set(false)
            jobA.join()
            jobB.join()
        } finally {
            toxA.close()
            toxB.close()
        }
    }

    @Test
    fun testSilentChunkTimeoutCleanup() = runTest {
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
            val isBConnected = AtomicBoolean(false)

            listenerA.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkB.string() && status != ConnectionStatus.None) isAConnected.set(true)
            }
            listenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) isBConnected.set(true)
            }

            val fileSize = 15360
            val fileRecvCalled = AtomicBoolean(false)
            val incomingFileNo = AtomicInteger(-1)

            listenerB.fileRecvHandler = { publicKey, fileNo, kind, size, name ->
                if (publicKey == pkA.string()) {
                    incomingFileNo.set(fileNo)
                    fileRecvCalled.set(true)
                    toxB.startFileTransfer(pkA, fileNo)
                }
            }

            val fileChunkRequestedCount = AtomicInteger(0)
            listenerA.fileChunkRequestHandler = { publicKey, fileNo, position, length ->
                if (publicKey == pkB.string()) {
                    fileChunkRequestedCount.incrementAndGet()
                }
            }

            val runLoops = AtomicBoolean(true)
            val jobA = launch { while (runLoops.get()) { toxA.iterate(); delay(10) } }
            val runLoopsB = AtomicBoolean(true)
            var jobB = launch { while (runLoopsB.get()) { toxB.iterate(); delay(10) } }

            toxA.bootstrap("127.0.0.1", toxB.selfGetUdpPort(), toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", toxA.selfGetUdpPort(), toxA.selfGetDhtId())

            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 20000L) {
                delay(100)
            }
            assertTrue(isAConnected.get() && isBConnected.get())

            val fileNo = toxA.sendFile(pkB, FileKind.Data, fileSize.toLong(), "timeout_test.bin")
            assertTrue(fileNo >= 0)

            delay(500)

            runLoopsB.set(false)
            jobB.join()
            toxB.close()

            delay(12000)

            toxA.stopFileTransfer(pkB, fileNo)

            runLoops.set(false)
            jobA.join()
        } finally {
            toxA.close()
        }
    }
}
