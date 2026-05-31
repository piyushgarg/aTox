// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.runtime.ToxWrapper
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.settings.model.ProxyType
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ToxLatencyStressTest {

    @Test
    fun testMainThreadIsNeverBlockedByJni() = runTest {
        val options = SaveOptions(null, false, ProxyType.None, "", 0)
        val tox = ToxWrapper(ToxEventListener(), ToxAvEventListener(), options)

        try {
            val uiFrameDelays = Collections.synchronizedList(mutableListOf<Long>())
            val stopUiLoop = AtomicBoolean(false)

            // 1. Start a simulated Main/UI thread loop representing rendering at 60fps (16ms interval)
            val uiJob = launch(Dispatchers.Main) {
                var lastTime = System.currentTimeMillis()
                while (!stopUiLoop.get()) {
                    delay(16)
                    val now = System.currentTimeMillis()
                    uiFrameDelays.add(now - lastTime)
                    lastTime = now
                }
            }

            // 2. Heavy JNI load executing on background thread (Dispatchers.Default)
            val jniJob = launch(Dispatchers.Default) {
                // Ensure we are not on the main thread
                assertNotEquals(Looper.getMainLooper().thread, Thread.currentThread())
                
                for (i in 0..100) {
                    tox.setName("User $i")
                    tox.getStatusMessage()
                    tox.getContacts()
                }
            }

            // Await JNI workload to complete
            jniJob.join()
            stopUiLoop.set(true)
            uiJob.join()

            // 3. Verify UI frame delays are smooth and have no heavy spikes/janks
            val maxJankMs = uiFrameDelays.maxOrNull() ?: 0L
            
            // We allow up to 45ms to accommodate GC spikes or CI runner scheduling overheads,
            // but ensuring it never blocks the UI for ANR thresholds (5000ms) or heavy locks.
            assertTrue(
                maxJankMs < 50L,
                "UI Jank detected! Main thread was blocked for $maxJankMs ms during active JNI operations"
            )

        } finally {
            tox.close()
        }
    }

    @Test
    fun testJniMutexContentionLatencyUnderHeavyLoad() = runTest {
        val options = SaveOptions(null, false, ProxyType.None, "", 0)
        val tox = ToxWrapper(ToxEventListener(), ToxAvEventListener(), options)

        try {
            val stopHeavyLoad = AtomicBoolean(false)

            // 1. Spawn background workers that continuously flood JNI with operations to create lock contention
            val workers = List(4) {
                launch(Dispatchers.Default) {
                    while (!stopHeavyLoad.get()) {
                        tox.getStatusMessage()
                        tox.getName()
                    }
                }
            }

            // 2. Measure the response time of a high-priority operation on another thread
            val latencies = mutableListOf<Long>()
            withContext(Dispatchers.Default) {
                for (i in 0..50) {
                    val start = System.currentTimeMillis()
                    tox.setName("Alice")
                    val duration = System.currentTimeMillis() - start
                    latencies.add(duration)
                    delay(5)
                }
            }

            stopHeavyLoad.set(true)
            workers.forEach { it.join() }

            // 3. Assert that lock contention is handled gracefully and max latency is under a frame duration
            val maxLatencyMs = latencies.maxOrNull() ?: 0L
            assertTrue(
                maxLatencyMs < 20L,
                "High JNI lock contention detected! Max JNI API call duration took $maxLatencyMs ms (should be < 20ms)"
            )

        } finally {
            tox.close()
        }
    }
}
