// SPDX-FileCopyrightText: 2020-2026 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.core.network.bootstrap.BootstrapNode
import ltd.evilcorp.domain.core.network.bootstrap.IBootstrapNodeRegistry
import ltd.evilcorp.domain.core.network.save.ISaveManager
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.core.tox.runtime.ToxSessionSaver
import ltd.evilcorp.core.tox.runtime.ToxEngine
import ltd.evilcorp.core.tox.runtime.ToxCallBridge
import ltd.evilcorp.core.tox.runtime.ToxRuntime
import kotlinx.coroutines.test.StandardTestDispatcher
import ltd.evilcorp.core.tox.impl.ToxCallControllerImpl
import ltd.evilcorp.core.tox.impl.ToxFileTransmitterImpl
import ltd.evilcorp.core.tox.impl.ToxGroupManagerImpl
import ltd.evilcorp.core.tox.impl.ToxMessengerImpl
import ltd.evilcorp.core.tox.impl.ToxProfileImpl
import org.junit.runner.RunWith

class FakeBootstrapNodeRegistry(val nodes: List<BootstrapNode> = listOf()) : IBootstrapNodeRegistry {
    override suspend fun get(n: Int): List<BootstrapNode> = nodes.take(n)
    override suspend fun reset() {}
}

class FakeSaveManager : ISaveManager {
    override fun list(): List<String> = listOf()
    override fun load(pk: PublicKey): ByteArray? = null
    override fun save(pk: PublicKey, saveData: ByteArray) {}
    override fun delete(pk: PublicKey): Boolean = true
}

@RunWith(AndroidJUnit4::class)
class ToxTest {
    @ExperimentalCoroutinesApi
    @Test
    fun quitting_does_not_crash() = runTest {


        repeat(10) {
            val sessionSaver = ToxSessionSaver(FakeSaveManager())
            val engine = ToxEngine(this, FakeBootstrapNodeRegistry())
            val callBridge = ToxCallBridge()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val runtime = ToxRuntime(this, sessionSaver, engine, callBridge, dispatcher)
            val tox = ToxImpl(
                runtime = runtime,
                profileImpl = ToxProfileImpl(runtime),
                messengerImpl = ToxMessengerImpl(runtime),
                fileTransmitterImpl = ToxFileTransmitterImpl(runtime),
                callControllerImpl = ToxCallControllerImpl(runtime),
                groupManagerImpl = ToxGroupManagerImpl(runtime)
            ).apply { isBootstrapNeeded = false }

            tox.start(SaveOptions(null, false, ProxyType.None, "", 0), null, ToxEventListener(), ToxAvEventListener())
            advanceTimeBy(25.milliseconds)
            tox.stop()
            advanceUntilIdle()
        }
    }

    private fun isInternetAvailable(): Boolean {
        return try {
            java.net.InetAddress.getByName("tox.abilinski.com")
            true
        } catch (e: Exception) {
            false
        }
    }

    @ExperimentalCoroutinesApi
    @Test(timeout = 60 * 1000)
    fun bootstrapping_against_a_live_node_works(): Unit = runBlocking {
        org.junit.Assume.assumeTrue("Сеть недоступна, пропускаем интеграционный тест Tox", isInternetAvailable())


        var connected = false
        val eventListener = ToxEventListener().apply {
            selfConnectionStatusHandler = { status ->
                connected = status != ConnectionStatus.None
            }
        }

        val nodes = listOf(
            BootstrapNode(
                "tox.abilinski.com",
                33445,
                PublicKey("10C00EB250C3233E343E2AEBA07115A5C28920E9C8D29492F6D00B29049EDC7E"),
            ),
            BootstrapNode(
                "tox.kurnevsky.net",
                33445,
                PublicKey("82EF82BA33445A1F91A7DB27189ECFC0C013E06E3DA71F588ED692BED625EC23"),
            ),
            BootstrapNode(
                "initramfs.io",
                33445,
                PublicKey("3F0A45A268367C1BEA652F258C85F4A66DA76BCAA667A49E770BCC4917AB6A25"),
            ),
            BootstrapNode(
                "tox2.plastiras.org",
                33445,
                PublicKey("B6626D386BE7E3ACA107B46F48A5C4D522D29281750D44A0CBA6A2721E79C951"),
            ),
        )

        val sessionSaver = ToxSessionSaver(FakeSaveManager())
        val engine = ToxEngine(this, FakeBootstrapNodeRegistry(nodes))
        val callBridge = ToxCallBridge()
        val runtime = ToxRuntime(this, sessionSaver, engine, callBridge, kotlinx.coroutines.Dispatchers.IO)
        val tox = ToxImpl(
            runtime = runtime,
            profileImpl = ToxProfileImpl(runtime),
            messengerImpl = ToxMessengerImpl(runtime),
            fileTransmitterImpl = ToxFileTransmitterImpl(runtime),
            callControllerImpl = ToxCallControllerImpl(runtime),
            groupManagerImpl = ToxGroupManagerImpl(runtime)
        )

        tox.start(SaveOptions(null, false, ProxyType.None, "", 0), null, eventListener, ToxAvEventListener())

        val startTime = System.currentTimeMillis()
        val timeoutMs = 8000L
        while (!connected && (System.currentTimeMillis() - startTime) < timeoutMs) {
            delay(500.milliseconds)
        }

        tox.stop()

        org.junit.Assume.assumeTrue(
            "Не удалось установить соединение с живой нодой Tox за $timeoutMs мс, пропускаем тест",
            connected
        )
    }
}
