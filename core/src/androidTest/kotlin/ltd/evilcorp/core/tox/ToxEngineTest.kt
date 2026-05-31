package ltd.evilcorp.core.tox

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.runtime.ToxEngine
import ltd.evilcorp.core.tox.runtime.ToxWrapper
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.bootstrap.BootstrapNode
import ltd.evilcorp.domain.core.network.bootstrap.IBootstrapNodeRegistry
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.settings.model.ProxyType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ToxEngineTest {

    private class FakeBootstrapNodeRegistry : IBootstrapNodeRegistry {
        var getCount = 0
        override suspend fun get(n: Int): List<BootstrapNode> {
            getCount++
            return listOf()
        }
        override suspend fun reset() {}
    }

    private lateinit var nodeRegistry: FakeBootstrapNodeRegistry
    private lateinit var toxWrapper: ToxWrapper

    @Before
    fun setUp() {
        nodeRegistry = FakeBootstrapNodeRegistry()
        toxWrapper = ToxWrapper(
            ToxEventListener(),
            ToxAvEventListener(),
            SaveOptions(null, false, ProxyType.None, "", 0)
        )
    }

    @After
    fun tearDown() {
        toxWrapper.close()
    }

    @Test
    fun testInitialState() = runTest {
        val engine = ToxEngine(this, nodeRegistry)
        assertFalse(engine.isRunning())
        assertTrue(engine.isBootstrapNeeded)
    }

    @Test
    fun testStart_and_stop_lifecycle() = runTest {
        val engine = ToxEngine(this, nodeRegistry)
        var onStoppedCalled = false
        
        assertFalse(engine.isRunning())
        engine.start(toxWrapper) {
            onStoppedCalled = true
        }

        assertTrue(engine.isRunning())
        
        // Let it run a bit and iterate
        delay(150.milliseconds)

        engine.stop()
        
        // Wait for iterate to stop with real timeout
        val startTime = System.currentTimeMillis()
        while (engine.isRunning() && (System.currentTimeMillis() - startTime) < 2000) {
            delay(10)
        }

        assertFalse(engine.isRunning())
        assertTrue(onStoppedCalled)
    }

    @Test
    fun testBootstrapTrigger() = runTest {
        val engine = ToxEngine(this, nodeRegistry)
        assertTrue(engine.isBootstrapNeeded)
        assertEquals(0, nodeRegistry.getCount)

        engine.start(toxWrapper) {}
        
        // Wait for iterate to execute bootstrap with real timeout
        val startTime = System.currentTimeMillis()
        while (engine.isBootstrapNeeded && (System.currentTimeMillis() - startTime) < 2000) {
            delay(10)
        }

        assertFalse(engine.isBootstrapNeeded)
        assertTrue(nodeRegistry.getCount > 0)

        engine.stop()
        advanceUntilIdle()
    }
}

