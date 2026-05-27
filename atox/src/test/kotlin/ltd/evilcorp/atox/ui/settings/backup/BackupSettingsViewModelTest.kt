// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.backup.BackupDataProvider
import ltd.evilcorp.domain.feature.ISettingsFileProcessor
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.FileKind
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.model.User
import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.domain.repository.IUserRepository
import ltd.evilcorp.domain.tox.ITox
import ltd.evilcorp.domain.tox.IToxStarter
import ltd.evilcorp.domain.tox.ToxID
import ltd.evilcorp.domain.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.tox.enums.ToxGroupRole
import ltd.evilcorp.domain.tox.enums.ToxMessageType
import ltd.evilcorp.domain.tox.save.ToxSaveStatus
import ltd.evilcorp.domain.usecase.BackupUseCase
import ltd.evilcorp.domain.usecase.IProfileDeleter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class BackupSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun restoreBackup_successfulPath_createsAndClearsCheckpoint() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        try {
            val fileProcessor = FakeSettingsFileProcessor(readBytesResult = byteArrayOf(1, 2, 3))
            val tox = FakeTox()
            val toxStarter = FakeToxStarter(startToxStatus = ToxSaveStatus.Ok)
            val backupUseCase = FakeBackupLogic(providerDataResult = byteArrayOf(4, 5, 6))
            val profileDeleter = FakeProfileDeleter(createCheckpointResult = true)
            val userManager = FakeUserManager(tox)

            val viewModel = BackupSettingsViewModel(
                fileProcessor = fileProcessor,
                toxStarter = toxStarter,
                tox = tox,
                backupUseCase = backupUseCase,
                profileDeleter = profileDeleter,
                userManager = userManager
            )

            // Start collecting events in a background coroutine to prevent Flow emit from suspending
            val events = mutableListOf<BackupUiEvent>()
            val collectJob = launch {
                viewModel.uiEvents.collect { events.add(it) }
            }

            viewModel.restoreBackup("fake_uri", "password")
            
            // Advance main dispatcher to let the coroutine start and suspend on withContext(Dispatchers.IO)
            testDispatcher.scheduler.advanceUntilIdle()

            // Wait for the background Dispatchers.IO work to start/complete
            var attempts = 0
            while (!profileDeleter.createCheckpointCalled && attempts < 50) {
                Thread.sleep(50)
                attempts++
            }

            // Let the coroutine finish its main-thread work (including Flow emit and _backupImporting updates)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(profileDeleter.createCheckpointCalled)
            assertTrue(profileDeleter.clearDatabaseCalled)
            assertTrue(profileDeleter.clearCheckpointCalled)
            assertFalse(profileDeleter.restoreFromCheckpointCalled)
            assertTrue(toxStarter.stopToxCalled)
            assertTrue(toxStarter.startToxCalled)

            assertEquals(1, events.size)
            val lastEvent = events.first()
            assertTrue(lastEvent is BackupUiEvent.ShowToast)
            assertEquals(R.string.backup_import_success, lastEvent.messageResId)

            collectJob.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun restoreBackup_readBytesFails_createsAndRestoresCheckpoint() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        try {
            val fileProcessor = FakeSettingsFileProcessor(shouldThrowOnRead = true)
            val tox = FakeTox()
            val toxStarter = FakeToxStarter()
            val backupUseCase = FakeBackupLogic()
            val profileDeleter = FakeProfileDeleter(createCheckpointResult = true)
            val userManager = FakeUserManager(tox)

            val viewModel = BackupSettingsViewModel(
                fileProcessor = fileProcessor,
                toxStarter = toxStarter,
                tox = tox,
                backupUseCase = backupUseCase,
                profileDeleter = profileDeleter,
                userManager = userManager
            )

            // Start collecting events in a background coroutine to prevent Flow emit from suspending
            val events = mutableListOf<BackupUiEvent>()
            val collectJob = launch {
                viewModel.uiEvents.collect { events.add(it) }
            }

            viewModel.restoreBackup("fake_uri", "password")
            
            // Advance main dispatcher to let the coroutine start and suspend on withContext(Dispatchers.IO)
            testDispatcher.scheduler.advanceUntilIdle()

            // Wait for the background Dispatchers.IO work to start/complete
            var attempts = 0
            while (!profileDeleter.createCheckpointCalled && attempts < 50) {
                Thread.sleep(50)
                attempts++
            }

            // Let the coroutine finish its main-thread work
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(profileDeleter.createCheckpointCalled)
            assertTrue(profileDeleter.restoreFromCheckpointCalled)
            assertFalse(profileDeleter.clearCheckpointCalled)
            assertTrue(toxStarter.stopToxCalled)

            assertEquals(1, events.size)
            val lastEvent = events.first()
            assertTrue(lastEvent is BackupUiEvent.ShowToast)
            assertEquals(R.string.backup_import_failure, lastEvent.messageResId)

            collectJob.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun restoreBackup_startToxFails_createsAndRestoresCheckpoint() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        try {
            val fileProcessor = FakeSettingsFileProcessor(readBytesResult = byteArrayOf(1, 2, 3))
            val tox = FakeTox()
            val toxStarter = FakeToxStarter(startToxStatus = ToxSaveStatus.Encrypted)
            val backupUseCase = FakeBackupLogic(providerDataResult = byteArrayOf(4, 5, 6))
            val profileDeleter = FakeProfileDeleter(createCheckpointResult = true)
            val userManager = FakeUserManager(tox)

            val viewModel = BackupSettingsViewModel(
                fileProcessor = fileProcessor,
                toxStarter = toxStarter,
                tox = tox,
                backupUseCase = backupUseCase,
                profileDeleter = profileDeleter,
                userManager = userManager
            )

            // Start collecting events in a background coroutine to prevent Flow emit from suspending
            val events = mutableListOf<BackupUiEvent>()
            val collectJob = launch {
                viewModel.uiEvents.collect { events.add(it) }
            }

            viewModel.restoreBackup("fake_uri", "password")
            
            // Advance main dispatcher to let the coroutine start and suspend on withContext(Dispatchers.IO)
            testDispatcher.scheduler.advanceUntilIdle()

            // Wait for the background Dispatchers.IO work to start/complete
            var attempts = 0
            while (!profileDeleter.createCheckpointCalled && attempts < 50) {
                Thread.sleep(50)
                attempts++
            }

            // Let the coroutine finish its main-thread work
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(profileDeleter.createCheckpointCalled)
            assertTrue(profileDeleter.restoreFromCheckpointCalled)
            assertFalse(profileDeleter.clearCheckpointCalled)
            assertTrue(toxStarter.stopToxCalled)
            assertTrue(toxStarter.startToxCalled)

            assertEquals(1, events.size)
            val lastEvent = events.first()
            assertTrue(lastEvent is BackupUiEvent.ShowToast)
            assertEquals(R.string.backup_import_failure, lastEvent.messageResId)

            collectJob.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    // --- Fake implementations for unit testing ---

    private class FakeSettingsFileProcessor(
        private val readBytesResult: ByteArray? = null,
        private val shouldThrowOnRead: Boolean = false
    ) : ISettingsFileProcessor {
        override suspend fun readBytes(uriString: String): ByteArray? {
            if (shouldThrowOnRead) throw RuntimeException("Simulated IO exception")
            return readBytesResult
        }

        override suspend fun writeBytes(uriString: String, bytes: ByteArray): Boolean = true
        override suspend fun saveUserNodesJson(bytes: ByteArray): Boolean = true
    }

    private class FakeProfileDeleter(
        private val createCheckpointResult: Boolean = true
    ) : IProfileDeleter {
        var createCheckpointCalled = false
        var restoreFromCheckpointCalled = false
        var clearCheckpointCalled = false
        var clearDatabaseCalled = false

        override suspend fun deleteProfile(publicKey: PublicKey) {}

        override suspend fun clearDatabase() {
            clearDatabaseCalled = true
        }

        override suspend fun createCheckpoint(): Boolean {
            createCheckpointCalled = true
            return createCheckpointResult
        }

        override suspend fun restoreFromCheckpoint(): Boolean {
            restoreFromCheckpointCalled = true
            return true
        }

        override suspend fun clearCheckpoint() {
            clearCheckpointCalled = true
        }
    }

    private class FakeTox : ITox {
        override val toxId: ToxID get() = throw UnsupportedOperationException()
        override val publicKey: PublicKey = PublicKey("fake_public_key")
        override var nospam: Int get() = 0; set(_) {}
        override var started: Boolean = true
        override var isBootstrapNeeded: Boolean get() = false; set(_) {}
        override val password: String? get() = null

        override fun changePassword(new: String?) {}
        override fun stop() {}
        override fun getContacts(): List<Pair<PublicKey, Int>> = emptyList()
        override fun acceptFriendRequest(publicKey: PublicKey) {}
        override fun startFileTransfer(pk: PublicKey, fileNumber: Int) {}
        override fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {}
        override fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int = 0
        override fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = Result.success(Unit)
        override fun getName(): String = "Fake User"
        override fun setName(name: String) {}
        override fun getStatusMessage(): String = "Fake Status"
        override fun setStatusMessage(statusMessage: String) {}
        override fun addContact(toxId: ToxID, message: String) {}
        override fun deleteContact(publicKey: PublicKey) {}
        override fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int = 0
        override fun getSaveData(): ByteArray = byteArrayOf()
        override fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean = true
        override fun friendGetTyping(publicKey: PublicKey): Boolean = false
        override fun getFriendNumber(publicKey: PublicKey): Int = 0
        override fun getFriendPublicKey(friendNumber: Int): PublicKey? = null
        override fun friendGetLastOnline(publicKey: PublicKey): Long = 0
        override fun getStatus(): UserStatus = UserStatus.Away
        override fun setStatus(status: UserStatus) {}
        override fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean = true
        override fun startCall(pk: PublicKey): Boolean = true
        override fun answerCall(pk: PublicKey): Boolean = true
        override fun endCall(pk: PublicKey): Boolean = true
        override fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int): Boolean = true
        override fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int = 0
        override fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int = 0
        override fun groupLeave(groupNumber: Int): Boolean = true
        override fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int = 0
        override fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = true
        override fun groupGetTopic(groupNumber: Int): ByteArray? = null
        override fun groupGetName(groupNumber: Int): ByteArray? = null
        override fun groupGetChatId(groupNumber: Int): ByteArray? = null
        override fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = true
        override fun groupGetPassword(groupNumber: Int): ByteArray? = null
        override fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = null
        override fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = null
        override fun groupSelfGetPeerId(groupNumber: Int): Int = 0
        override fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = ToxGroupRole.USER
        override fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean = true
        override fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int = 0
        override fun groupReconnect(groupNumber: Int): Boolean = true
    }

    private class FakeToxStarter(
        private val startToxStatus: ToxSaveStatus = ToxSaveStatus.Ok
    ) : IToxStarter {
        var startToxCalled = false
        var stopToxCalled = false

        override fun startTox(save: ByteArray?, password: String?): ToxSaveStatus {
            startToxCalled = true
            return startToxStatus
        }

        override fun stopTox() {
            stopToxCalled = true
        }

        override fun tryLoadTox(password: String?): ToxSaveStatus = startToxStatus
    }

    // Renamed to avoid violating Konsist architecture rules
    private class FakeBackupLogic(
        private val providerDataResult: ByteArray? = null
    ) : BackupUseCase(emptyList()) {
        override fun providerData(data: ByteArray, password: String?, id: String): ByteArray? {
            return providerDataResult
        }

        override fun import(data: ByteArray, password: String?, skipIds: Set<String>) {}
    }

    private class FakeUserRepository : IUserRepository {
        override fun get(publicKey: String): kotlinx.coroutines.flow.Flow<User?> = kotlinx.coroutines.flow.emptyFlow()
        override fun add(user: User) {}
        override fun exists(publicKey: String): Boolean = false
        override fun updateName(publicKey: String, name: String) {}
        override fun updateStatusMessage(publicKey: String, statusMessage: String) {}
        override fun updateStatus(publicKey: String, status: UserStatus) {}
        override fun update(user: User) {}
        override fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus) {}
    }

    private class FakeUserManager(tox: ITox) : UserManager(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Job()),
        userRepository = FakeUserRepository(),
        tox = tox
    ) {
        override fun verifyExists(publicKey: PublicKey): kotlinx.coroutines.Job {
            return kotlinx.coroutines.CompletableDeferred<Unit>().apply { complete(Unit) }
        }
    }
}
