package ltd.evilcorp.core.tox

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.tox.impl.ToxCallControllerImpl
import ltd.evilcorp.core.tox.impl.ToxFileTransmitterImpl
import ltd.evilcorp.core.tox.impl.ToxGroupManagerImpl
import ltd.evilcorp.core.tox.impl.ToxMessengerImpl
import ltd.evilcorp.core.tox.impl.ToxProfileImpl
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.runtime.ToxCallBridge
import ltd.evilcorp.core.tox.runtime.ToxEngine
import ltd.evilcorp.core.tox.runtime.ToxRuntime
import ltd.evilcorp.core.tox.runtime.ToxSessionSaver
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.bootstrap.BootstrapNode
import ltd.evilcorp.domain.core.network.bootstrap.IBootstrapNodeRegistry
import ltd.evilcorp.domain.core.network.save.ISaveManager
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.settings.model.ProxyType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ToxImplTest {

    private class FakeSaveManager : ISaveManager {
        override fun list(): List<String> = listOf()
        override fun load(pk: PublicKey): ByteArray? = null
        override fun save(pk: PublicKey, saveData: ByteArray) {}
        override fun delete(pk: PublicKey): Boolean = true
    }

    private class FakeBootstrapNodeRegistry : IBootstrapNodeRegistry {
        override suspend fun get(n: Int): List<BootstrapNode> = listOf()
        override suspend fun reset() {}
    }

    private lateinit var sessionSaver: ToxSessionSaver
    private lateinit var callBridge: ToxCallBridge

    @Before
    fun setUp() {
        sessionSaver = ToxSessionSaver(FakeSaveManager())
        callBridge = ToxCallBridge()
    }

    @After
    fun tearDown() {
        // No-op
    }

    private fun TestScope.createTox(): ToxImpl {
        val engine = ToxEngine(this, FakeBootstrapNodeRegistry())
        val dispatcher = StandardTestDispatcher(testScheduler)
        val runtime = ToxRuntime(this, sessionSaver, engine, callBridge, dispatcher)
        return ToxImpl(
            runtime = runtime,
            profileImpl = ToxProfileImpl(runtime),
            messengerImpl = ToxMessengerImpl(runtime),
            fileTransmitterImpl = ToxFileTransmitterImpl(runtime),
            callControllerImpl = ToxCallControllerImpl(runtime),
            groupManagerImpl = ToxGroupManagerImpl(runtime)
        ).apply { isBootstrapNeeded = false }
    }

    @Test
    fun testInitialState() = runTest {
        val tox = createTox()

        assertFalse(tox.started)
        assertFalse(tox.isBootstrapNeeded)
    }

    @Test
    fun testStart_and_stop() = runTest {
        val tox = createTox()

        assertFalse(tox.started)
        
        tox.start(
            SaveOptions(null, false, ProxyType.None, "", 0),
            null,
            ToxEventListener(),
            ToxAvEventListener()
        )
        
        assertTrue(tox.started)
        assertNotNull(tox.toxId)
        assertNotNull(tox.publicKey)

        tox.stop()
        advanceUntilIdle()
        assertFalse(tox.started)
    }

    @Test
    fun testGetSetName_and_status() = runTest {
        val tox = createTox()

        tox.start(
            SaveOptions(null, false, ProxyType.None, "", 0),
            null,
            ToxEventListener(),
            ToxAvEventListener()
        )

        // Initial default name is empty in fresh Tox instance
        assertEquals("", tox.getName())

        tox.setName("aTox User")
        assertEquals("aTox User", tox.getName())

        assertEquals("", tox.getStatusMessage())
        tox.setStatusMessage("Coding in Kotlin")
        assertEquals("Coding in Kotlin", tox.getStatusMessage())

        assertEquals(UserStatus.None, tox.getStatus())
        tox.setStatus(UserStatus.Away)
        assertEquals(UserStatus.Away, tox.getStatus())

        tox.stop()
        advanceUntilIdle()
    }
}
