package ltd.evilcorp.domain.features.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.fakes.FakeContactRepository
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import ltd.evilcorp.domain.fakes.FakeTox
import ltd.evilcorp.domain.fakes.FakeCallSessionRegistry
import ltd.evilcorp.domain.features.call.service.ICallSignalPlayer
import ltd.evilcorp.domain.features.call.service.IAudioRecorder
import ltd.evilcorp.domain.features.call.usecase.LogCallUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CallManagerTest {

    private val signalPlayer = object : ICallSignalPlayer {
        var ringtonePlaying = false
        var ringbackPlaying = false
        override fun playIncomingRingtone(scope: CoroutineScope) { ringtonePlaying = true }
        override fun playRingback(scope: CoroutineScope, isCallActive: () -> Boolean) { ringbackPlaying = true }
        override fun stopSignals() {
            ringtonePlaying = false
            ringbackPlaying = false
        }
    }

    private val audioRecorder = object : IAudioRecorder {
        override val sendingAudio = MutableStateFlow(false)
        override fun startAudioCapture(scope: CoroutineScope, to: PublicKey, speakerphoneOn: Boolean, isTargetValid: () -> Boolean) {
            sendingAudio.value = true
        }
        override fun stopAudioCapture() {
            sendingAudio.value = false
        }
        override fun isRecordingActive(): Boolean = sendingAudio.value
        override suspend fun joinRecording() {}
    }

    private val audioRoutingManager = object : IAudioRoutingManager {
        var audioFocusActive = false
        var isCommunicationModeActive = false
        var isSpeakerphoneRouteActive = false
        override fun requestCallAudioFocus(onFocusLoss: () -> Unit, onFocusGain: () -> Unit): Boolean {
            audioFocusActive = true
            return true
        }
        override fun abandonCallAudioFocus() {
            audioFocusActive = false
        }
        override fun setCommunicationMode(active: Boolean) {
            isCommunicationModeActive = active
        }
        override fun setSpeakerphoneRoute(on: Boolean) {
            isSpeakerphoneRouteActive = on
        }
    }

    @Test
    fun `startOutgoingCall successfully starts call and changes state`() = runTest {
        // Arrange
        val tox = FakeTox()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val logCallUseCase = LogCallUseCase(messageRepo)
        
        val mediaCoordinator = CallMediaCoordinator(signalPlayer, audioRecorder, audioRoutingManager)
        val sessionRegistry = FakeCallSessionRegistry()

        val contactPk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        contactRepo.add(Contact(contactPk.string(), connectionStatus = ConnectionStatus.UDP))

        val manager = CallManager(tox, scope, contactRepo, logCallUseCase, mediaCoordinator, sessionRegistry)

        // Act
        val started = manager.startOutgoingCall(contactPk)

        // Assert
        assertTrue(started)
        assertTrue(sessionRegistry.inCall.value is CallState.OutgoingRequesting)
        assertTrue(signalPlayer.ringbackPlaying)
        assertTrue(audioRoutingManager.audioFocusActive)
        assertTrue(audioRoutingManager.isCommunicationModeActive)
    }

    @Test
    fun `onIncomingCall changes state and plays ringtone`() = runTest {
        // Arrange
        val tox = FakeTox()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val logCallUseCase = LogCallUseCase(messageRepo)
        
        val mediaCoordinator = CallMediaCoordinator(signalPlayer, audioRecorder, audioRoutingManager)
        val sessionRegistry = FakeCallSessionRegistry()

        val contactPk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val contact = Contact(contactPk.string())

        val manager = CallManager(tox, scope, contactRepo, logCallUseCase, mediaCoordinator, sessionRegistry)

        // Act
        manager.onIncomingCall(contact)

        // Assert
        assertTrue(sessionRegistry.inCall.value is CallState.IncomingRinging)
        assertTrue(signalPlayer.ringtonePlaying)
    }

    @Test
    fun `endCall hangs up call and logs history`() = runTest {
        // Arrange
        val tox = FakeTox()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val logCallUseCase = LogCallUseCase(messageRepo)
        
        val mediaCoordinator = CallMediaCoordinator(signalPlayer, audioRecorder, audioRoutingManager)
        val sessionRegistry = FakeCallSessionRegistry()

        val contactPk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        contactRepo.add(Contact(contactPk.string()))
        sessionRegistry.setCallState(CallState.Active(contactPk, 0L, 0L, outgoing = true))

        val manager = CallManager(tox, scope, contactRepo, logCallUseCase, mediaCoordinator, sessionRegistry)

        // Act
        manager.endCall(contactPk)

        // Assert
        assertEquals(CallState.Idle, sessionRegistry.inCall.value)
        assertFalse(audioRoutingManager.audioFocusActive)
        assertFalse(audioRoutingManager.isCommunicationModeActive)
        assertFalse(signalPlayer.ringbackPlaying)

        // Check call logged in history
        val msgs = messageRepo.get(contactPk.string()).first()
        assertEquals(1, msgs.size)
        assertEquals("[CALL_HISTORY_OUTGOING]", msgs[0].message)
    }

    @Test
    fun `terminate or endCall after already idle does not log duplicate call history`() = runTest {
        // Arrange
        val tox = FakeTox()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val logCallUseCase = LogCallUseCase(messageRepo)
        
        val mediaCoordinator = CallMediaCoordinator(signalPlayer, audioRecorder, audioRoutingManager)
        val sessionRegistry = FakeCallSessionRegistry()

        val contactPk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        contactRepo.add(Contact(contactPk.string(), connectionStatus = ConnectionStatus.UDP))

        val manager = CallManager(tox, scope, contactRepo, logCallUseCase, mediaCoordinator, sessionRegistry)

        // Set call active
        sessionRegistry.setCallState(
            CallState.Active(contactPk, System.currentTimeMillis(), System.currentTimeMillis(), outgoing = true)
        )

        // Act: End call locally (first termination)
        manager.endCall(contactPk)

        // Assert first log is recorded
        val msgs = messageRepo.get(contactPk.string()).first()
        assertEquals(1, msgs.size, "Exactly one call log message should be recorded")

        // Act: Trigger remote terminate (e.g. from JNI callbacks saying call finished)
        manager.terminate(contactPk)

        // Assert: No second log is recorded (still 1)
        val msgsAfterTerminate = messageRepo.get(contactPk.string()).first()
        assertEquals(1, msgsAfterTerminate.size, "Call log should not be duplicated")
    }

    @Test
    fun `onIncomingCall during active call immediately rejects with busy`() = runTest {
        // Arrange
        val endedCalls = mutableListOf<PublicKey>()
        val tox = object : FakeTox() {
            override fun endCall(pk: PublicKey): Boolean {
                endedCalls.add(pk)
                return true
            }
        }
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val messageRepo = FakeMessageRepository()
        val logCallUseCase = LogCallUseCase(messageRepo)
        
        val mediaCoordinator = CallMediaCoordinator(signalPlayer, audioRecorder, audioRoutingManager)
        val sessionRegistry = FakeCallSessionRegistry()

        val activePk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val secondPk = PublicKey("5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C3982B009845B210C")

        val manager = CallManager(tox, scope, contactRepo, logCallUseCase, mediaCoordinator, sessionRegistry)

        // Set call active with the first peer
        sessionRegistry.setCallState(
            CallState.Active(activePk, System.currentTimeMillis(), System.currentTimeMillis(), outgoing = true)
        )

        // Act: Another peer (Charlie) calls us while we are busy on the first call
        val incomingContact = Contact(secondPk.string(), name = "Charlie")
        manager.onIncomingCall(incomingContact)

        // Assert: Charlie's call is immediately rejected via JNI endCall
        assertTrue(endedCalls.contains(secondPk), "The incoming call must be immediately rejected")
        
        // Assert: The active call remains active with the first peer
        val activeCall = sessionRegistry.inCall.value as CallState.Active
        assertEquals(activePk, activeCall.publicKey)
    }
}
