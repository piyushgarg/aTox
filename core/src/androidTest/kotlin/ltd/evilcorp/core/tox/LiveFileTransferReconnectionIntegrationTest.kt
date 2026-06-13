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
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LiveFileTransferReconnectionIntegrationTest {

    @Suppress("LongMethod")
    @Test
    fun testLiveFileTransferInterruptionAndRecovery() = runBlocking {
        // Allow lingering port resources from previous runs/tests to be fully released by the OS
        delay(2000)

        val options = SaveOptions(null, true, ProxyType.None, "", 0)

        // 1. Initialize Alice and Bob native instances
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

            // File transfer setup
            val fileSize = 5120 // 5 KB
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

            val receivedBytes = AtomicInteger(0)
            val fileTransferCompleted = AtomicBoolean(false)

            listenerB.fileRecvChunkHandler = { publicKey, fileNo, position, data ->
                if (publicKey == pkA.string() && fileNo == incomingFileNo.get()) {
                    val total = receivedBytes.addAndGet(data.size)
                    if (total >= fileSize) {
                        fileTransferCompleted.set(true)
                    }
                }
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
            toxA.bootstrap("127.0.0.1", portB, toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", portA, toxA.selfGetDhtId())

            // Wait to connect (increased timeout to 15 seconds for emulator environment)
            val startTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - startTime) < 15000L) {
                delay(100)
            }
            assertTrue(isAConnected.get())

            // Start sending file
            val fileNo = toxA.sendFile(pkB, FileKind.Data, fileSize.toLong(), "recovery_test.bin")
            assertTrue(fileNo >= 0)

            // Let Bob accept and receive a few chunks, then interrupt!
            delay(300)

            // -------------------------------------------------------------
            // SCENARIO: Bob goes offline midway through transfer
            // -------------------------------------------------------------
            val saveB = toxB.getSaveData()

            runLoopB.set(false)
            jobB.join()
            toxB.close()

            isAConnected.set(false)
            isBConnected.set(false)

            // Wait to register disconnection (exceed Toxcore's 10-second friend offline timeout)
            delay(12000)

            // Reset flags for recovery
            fileRecvCalled.set(false)
            fileTransferCompleted.set(false)
            receivedBytes.set(0)

            // Re-instantiate Bob
            val optionB = SaveOptions(saveB, true, ProxyType.None, "", 0)
            val newListenerB = ToxEventListener()
            val newAvListenerB = ToxAvEventListener()
            toxB = ToxWrapper(newListenerB, newAvListenerB, optionB)

            newListenerB.fileRecvHandler = { publicKey, fNo, kind, size, name ->
                if (publicKey == pkA.string()) {
                    incomingFileNo.set(fNo)
                    fileRecvCalled.set(true)
                    toxB.startFileTransfer(pkA, fNo)
                }
            }

            newListenerB.fileRecvChunkHandler = { publicKey, fNo, position, data ->
                if (publicKey == pkA.string() && fNo == incomingFileNo.get()) {
                    val total = receivedBytes.addAndGet(data.size)
                    if (total >= fileSize) {
                        fileTransferCompleted.set(true)
                    }
                }
            }

            newListenerB.friendConnectionStatusHandler = { pk, status ->
                if (pk == pkA.string() && status != ConnectionStatus.None) isBConnected.set(true)
            }

            runLoopB.set(true)
            jobB = launchBobLoop()

            // Re-bootstrap
            val recPortA = toxA.selfGetUdpPort()
            val recPortB = toxB.selfGetUdpPort()
            toxA.bootstrap("127.0.0.1", recPortB, toxB.selfGetDhtId())
            toxB.bootstrap("127.0.0.1", recPortA, toxA.selfGetDhtId())

            // Wait to reconnect (increased timeout to 25 seconds for robust DHT convergence)
            val reconnectTime = System.currentTimeMillis()
            while ((!isAConnected.get() || !isBConnected.get()) && (System.currentTimeMillis() - reconnectTime) < 25000L) {
                delay(100)
            }
            assertTrue(isAConnected.get())

            // Resend/resume the file transfer after connection recovery
            var newFileNo = -1
            val startResendTime = System.currentTimeMillis()
            while (newFileNo < 0 && (System.currentTimeMillis() - startResendTime) < 3000L) {
                newFileNo = toxA.sendFile(pkB, FileKind.Data, fileSize.toLong(), "recovery_test.bin")
                if (newFileNo < 0) delay(200)
            }
            assertTrue(newFileNo >= 0)

            // Wait for completion after recovery
            val transTime = System.currentTimeMillis()
            while (!fileTransferCompleted.get() && (System.currentTimeMillis() - transTime) < 5000L) {
                delay(100)
            }
            assertTrue(fileTransferCompleted.get(), "File transfer should complete successfully after reconnection")

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
