package ltd.evilcorp.core.tox

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.runtime.ToxCallBridge
import ltd.evilcorp.core.tox.runtime.ToxWrapper
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.settings.model.ProxyType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
class ToxCallBridgeTest {

    private lateinit var toxWrapper: ToxWrapper
    private lateinit var callBridge: ToxCallBridge

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        toxWrapper = ToxWrapper(
            ToxEventListener(),
            ToxAvEventListener(),
            SaveOptions(null, false, ProxyType.None, "", 0)
        )
        callBridge = ToxCallBridge()
    }

    @After
    fun tearDown() {
        toxWrapper.close()
    }

    @Test
    fun testWrapperThrowsIfUninitialized() {
        // Before init, calling startCall should throw IllegalStateException
        assertFailsWith<IllegalStateException> {
            callBridge.startCall(PublicKey("1234567890ABCDEF"))
        }
    }

    @Test
    fun testDelegatedCallMethods_whenNoActiveCall() {
        callBridge.init(toxWrapper)
        val pk = PublicKey("1234567890ABCDEF")

        // Without an active call session, these should return false/fail gracefully
        assertFalse(callBridge.startCall(pk))
        assertFalse(callBridge.answerCall(pk))
        assertFalse(callBridge.endCall(pk))
        assertFalse(callBridge.sendAudio(pk, shortArrayOf(0, 0, 0), 1, 16000))
        assertFalse(callBridge.sendVideoFrame(pk, 100, 100, byteArrayOf(), byteArrayOf(), byteArrayOf()))
        assertFalse(callBridge.audioSetBitRate(pk, 32))
        assertFalse(callBridge.videoSetBitRate(pk, 512))
    }
}
