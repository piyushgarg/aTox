package ltd.evilcorp.domain.features.call.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.INotificationManager
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.call.*
import ltd.evilcorp.domain.features.call.service.IAudioRecorder
import ltd.evilcorp.domain.features.call.service.ICallSignalPlayer
import ltd.evilcorp.domain.fakes.FakeCallSessionRegistry
import ltd.evilcorp.domain.fakes.FakeContactRepository
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import ltd.evilcorp.domain.fakes.FakeTox
import kotlin.test.*

class CallUseCasesTest {

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
        override fun startAudioCapture(
            scope: CoroutineScope,
            to: PublicKey,
            speakerphoneOn: Boolean,
            isTargetValid: () -> Boolean
        ) {
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

    private val notificationManager = object : INotificationManager {
        var ongoingCallContact: Contact? = null
        var dismissedCallPk: PublicKey? = null
        override fun showOngoingCallNotification(contact: Contact) {
            ongoingCallContact = contact
        }
        override fun dismissCallNotification(publicKey: PublicKey) {
            dismissedCallPk = publicKey
        }
    }

    private val proximityManager = object : IProximityManager {
        var acquired = false
        override fun acquire() {
            acquired = true
        }
        override fun release() {
            acquired = false
        }
    }

    private val tox = FakeTox()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val contactRepo = FakeContactRepository()
    private val messageRepo = FakeMessageRepository()
    private val logCallUseCase = LogCallUseCase(messageRepo)
    private val mediaCoordinator = CallMediaCoordinator(signalPlayer, audioRecorder, audioRoutingManager)
    private val sessionRegistry = FakeCallSessionRegistry()
    private val callManager = CallManager(tox, scope, contactRepo, logCallUseCase, mediaCoordinator, sessionRegistry)

    @Test
    fun `GetCallStateUseCase delegates to CallManager`() = runTest {
        val useCase = GetCallStateUseCase(callManager)
        assertEquals(CallState.Idle, useCase.inCall.value)

        sessionRegistry.setCallState(CallState.Connecting(PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C"), 0L, true))
        assertEquals(sessionRegistry.inCall.value, useCase.inCall.value)
    }

    @Test
    fun `GetMicrophoneStateUseCase delegates to CallManager`() = runTest {
        val useCase = GetMicrophoneStateUseCase(callManager)
        sessionRegistry.setMicrophoneEnabled(false)
        assertFalse(useCase.microphoneEnabled.value)

        callManager.startSendingAudio()
        assertTrue(useCase.microphoneEnabled.value)
    }

    @Test
    fun `GetSpeakerphoneStateUseCase delegates to CallManager`() = runTest {
        val useCase = GetSpeakerphoneStateUseCase(callManager)
        assertFalse(useCase.speakerphoneOnState.value)

        callManager.toggleSpeakerphone()
        assertTrue(useCase.speakerphoneOnState.value)
    }

    @Test
    fun `ManageCallUseCase handles StartOutgoingCall action`() = runTest {
        val useCase = ManageCallUseCase(callManager, notificationManager, proximityManager)
        val contactPk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val contact = Contact(contactPk.string(), connectionStatus = ConnectionStatus.UDP)
        contactRepo.add(contact)

        val result = useCase.execute(CallAction.StartOutgoingCall(contactPk, contact))
        assertTrue(result)
        assertEquals(contact, notificationManager.ongoingCallContact)
        assertTrue(callManager.microphoneEnabled.value)
    }

    @Test
    fun `ManageCallUseCase handles EndCall action`() = runTest {
        val useCase = ManageCallUseCase(callManager, notificationManager, proximityManager)
        val contactPk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        sessionRegistry.setCallState(CallState.Active(contactPk, 0L, 0L, outgoing = true))

        val result = useCase.execute(CallAction.EndCall(contactPk))
        assertTrue(result)
        assertEquals(contactPk, notificationManager.dismissedCallPk)
        assertEquals(CallState.Idle, sessionRegistry.inCall.value)
    }

    @Test
    fun `ManageCallUseCase handles StartSendingAudio and StopSendingAudio actions`() = runTest {
        val useCase = ManageCallUseCase(callManager, notificationManager, proximityManager)
        val contactPk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        sessionRegistry.setCallState(CallState.Active(contactPk, 0L, 0L, outgoing = true))

        assertTrue(useCase.execute(CallAction.StartSendingAudio))
        assertTrue(audioRecorder.sendingAudio.value)

        assertTrue(useCase.execute(CallAction.StopSendingAudio))
        assertFalse(audioRecorder.sendingAudio.value)
    }

    @Test
    fun `ManageCallUseCase handles ToggleSpeakerphone action`() = runTest {
        val useCase = ManageCallUseCase(callManager, notificationManager, proximityManager)

        assertFalse(callManager.speakerphoneOnState.value)
        
        // Act: Toggle speakerphone ON
        assertTrue(useCase.execute(CallAction.ToggleSpeakerphone))
        assertTrue(callManager.speakerphoneOnState.value)
        assertFalse(proximityManager.acquired) // released when speakerphone is ON

        // Act: Toggle speakerphone OFF
        assertTrue(useCase.execute(CallAction.ToggleSpeakerphone))
        assertFalse(callManager.speakerphoneOnState.value)
        assertTrue(proximityManager.acquired) // acquired when speakerphone is OFF
    }
}
