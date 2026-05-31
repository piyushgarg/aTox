package ltd.evilcorp.core.platform.storage

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.call.CallState
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CallSessionRegistryImplTest {

    private lateinit var registry: CallSessionRegistryImpl

    @BeforeTest
    fun setUp() {
        registry = CallSessionRegistryImpl()
    }

    @Test
    fun testInitialStates() = runTest {
        assertEquals(CallState.Idle, registry.inCall.value)
        assertFalse(registry.speakerphoneOn.value)
        assertTrue(registry.microphoneEnabled.value)
    }

    @Test
    fun testSetCallState() = runTest {
        val activeState = CallState.Active(
            publicKey = PublicKey("1234567890ABCDEF"),
            startedAt = 1000L,
            connectedAt = 2000L,
            outgoing = true
        )
        
        registry.setCallState(activeState)
        assertEquals(activeState, registry.inCall.first())
    }

    @Test
    fun testSetSpeakerphoneOn() = runTest {
        registry.setSpeakerphoneOn(true)
        assertTrue(registry.speakerphoneOn.first())
        
        registry.setSpeakerphoneOn(false)
        assertFalse(registry.speakerphoneOn.first())
    }

    @Test
    fun testSetMicrophoneEnabled() = runTest {
        registry.setMicrophoneEnabled(false)
        assertFalse(registry.microphoneEnabled.first())
        
        registry.setMicrophoneEnabled(true)
        assertTrue(registry.microphoneEnabled.first())
    }
}
