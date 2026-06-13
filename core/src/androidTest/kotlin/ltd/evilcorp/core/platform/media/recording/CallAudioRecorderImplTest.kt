// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.media.recording

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CallAudioRecorderImplTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val targetKey = PublicKey("76518406F6A9F2217E8DC487CC783C25CC16A15EB36FF32E335A235342C48A39")

    // Thread-safe FakeTox tracking audio sends
    private class FakeTox : ITox {
        override var started: Boolean = true
        override var isBootstrapNeeded: Boolean = false
        override val password: String? = null
        override val sessionId: String? = null
        override val toxId: ToxID = ToxID("76518406F6A9F2217E8DC487CC783C25CC16A15EB36FF32E335A235342C48A39000000008BE4")
        override val publicKey: PublicKey = PublicKey("76518406F6A9F2217E8DC487CC783C25CC16A15EB36FF32E335A235342C48A39")
        override var nospam: Int = 0

        val sentAudioFrames = CopyOnWriteArrayList<ShortArray>()

        override fun changePassword(new: String?) {}
        override fun stop() {}
        override fun getSaveData(): ByteArray = byteArrayOf()

        override fun getContacts(): List<Pair<PublicKey, Int>> = emptyList()
        override fun acceptFriendRequest(publicKey: PublicKey): Result<Unit> = Result.success(Unit)
        override fun addFriendNoRequest(publicKey: PublicKey): Int = 0
        override fun startFileTransfer(pk: PublicKey, fileNumber: Int) {}
        override fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {}
        override fun sendFile(pk: PublicKey, fileKind: ltd.evilcorp.domain.features.transfer.model.FileKind, fileSize: Long, fileName: String): Int = 0
        override fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = Result.success(Unit)
        override fun getName(): String = "TestUser"
        override fun setName(name: String) {}
        override fun getStatusMessage(): String = ""
        override fun setStatusMessage(statusMessage: String) {}
        override fun addContact(toxId: ToxID, message: String) {}
        override fun deleteContact(publicKey: PublicKey) {}
        override fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int = 0
        override fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean = true
        override fun friendGetTyping(publicKey: PublicKey): Boolean = false
        override fun getFriendNumber(publicKey: PublicKey): Int = -1
        override fun getFriendPublicKey(friendNumber: Int): PublicKey? = null
        override fun friendGetLastOnline(publicKey: PublicKey): Long = 0
        override fun getStatus(): UserStatus = UserStatus.None
        override fun setStatus(status: UserStatus) {}
        override fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean = true
        override fun startCall(pk: PublicKey): Boolean = true
        override fun answerCall(pk: PublicKey): Boolean = true
        override fun endCall(pk: PublicKey): Boolean = true
        
        override fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int): Boolean {
            sentAudioFrames.add(pcm.clone())
            return true
        }

        override fun groupNew(privacyState: ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int = 0
        override fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int = 0
        override fun groupLeave(groupNumber: Int): Boolean = true
        override fun groupSendMessage(groupNumber: Int, type: ltd.evilcorp.domain.core.network.enums.ToxMessageType, message: ByteArray): Int = 0
        override fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = true
        override fun groupGetTopic(groupNumber: Int): ByteArray? = null
        override fun groupGetName(groupNumber: Int): ByteArray? = null
        override fun groupGetChatId(groupNumber: Int): ByteArray? = byteArrayOf(1, 2, 3)
        override fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = true
        override fun groupGetPassword(groupNumber: Int): ByteArray? = null
        override fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = null
        override fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = null
        override fun groupSelfGetPeerId(groupNumber: Int): Int = 0
        
        override fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = ToxGroupRole.USER
        override fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean = true
        override fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int = 0
        override fun groupReconnect(groupNumber: Int): Boolean = true
        override fun groupGetChatlist(): IntArray = intArrayOf()
    }

    @Test
    fun testAudioCaptureLifecycle() = runTest {
        val fakeTox = FakeTox()
        val recorder = CallAudioRecorderImpl(fakeTox)

        assertFalse(recorder.sendingAudio.value, "Initially sendingAudio should be false")
        assertFalse(recorder.isRecordingActive(), "Initially isRecordingActive should be false")

        // Start capture on a test coroutine scope
        recorder.startAudioCapture(
            scope = this,
            to = targetKey,
            speakerphoneOn = false,
            isTargetValid = { true }
        )

        // Verify status flags
        assertTrue(recorder.sendingAudio.value, "sendingAudio should be true after starting")
        assertTrue(recorder.isRecordingActive(), "isRecordingActive should be true after starting")

        // Wait a small amount of time for some audio read/send cycles to trigger
        val startTime = System.currentTimeMillis()
        while (fakeTox.sentAudioFrames.isEmpty() && System.currentTimeMillis() - startTime < 1000L) {
            kotlinx.coroutines.delay(10)
        }

        // Verify we actually captured and sent audio frames
        assertTrue(fakeTox.sentAudioFrames.isNotEmpty(), "Tox should have received sent audio frames")
        
        // Stop audio capture
        recorder.stopAudioCapture()
        
        // Wait for recording thread job to clean up
        recorder.joinRecording()

        assertFalse(recorder.sendingAudio.value, "sendingAudio should be false after stopping")
        assertFalse(recorder.isRecordingActive(), "isRecordingActive should be false after stopping")
    }

    @Test
    fun testStopAudioCaptureFlow() = runTest {
        val fakeTox = FakeTox()
        val recorder = CallAudioRecorderImpl(fakeTox)

        // Start capture
        recorder.startAudioCapture(
            scope = this,
            to = targetKey,
            speakerphoneOn = true,
            isTargetValid = { true }
        )

        assertTrue(recorder.isRecordingActive(), "Recording should start")

        // Stop capture
        recorder.stopAudioCapture()
        recorder.joinRecording()

        assertFalse(recorder.isRecordingActive(), "Recording should stop")
        assertEquals(0, fakeTox.sentAudioFrames.size) // No frames should be sent after stopping
    }
}
