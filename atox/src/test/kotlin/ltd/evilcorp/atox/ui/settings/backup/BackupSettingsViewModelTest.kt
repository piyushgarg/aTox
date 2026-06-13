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
import ltd.evilcorp.domain.features.settings.ISettingsFileProcessor
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.auth.repository.IUserRepository
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.IToxStarter
import ltd.evilcorp.core.tox.FakeTox
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.backup.usecase.ImportBackupUseCase
import ltd.evilcorp.domain.features.backup.usecase.GetBackupProviderDataUseCase
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository
import ltd.evilcorp.domain.features.auth.model.ProfileInfo
import ltd.evilcorp.domain.features.settings.usecase.GetToxRunningStateUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.auth.usecase.VerifyProfileExistsUseCase
import ltd.evilcorp.domain.features.auth.usecase.ClearDatabaseUseCase
import ltd.evilcorp.domain.features.auth.usecase.ManageProfileCheckpointUseCase
import ltd.evilcorp.domain.features.settings.usecase.StartToxUseCase
import ltd.evilcorp.domain.features.settings.usecase.ManageToxLifecycleUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class BackupSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakePlatformServices = object : ltd.evilcorp.domain.core.platform.IPlatformServices {
        override fun formatDate(timestamp: Long): String = ""
        override fun generateSecureBytes(size: Int): ByteArray = ByteArray(size)
        override fun zip(files: Map<String, ByteArray>): ByteArray = ByteArray(0)
        override fun unzip(zipBytes: ByteArray): Map<String, ByteArray> = emptyMap()
    }

    @org.junit.Before
    fun setup() {
        io.mockk.mockkStatic(androidx.work.WorkManager::class)
        io.mockk.mockkObject(androidx.work.WorkManager.Companion)
        val mockWorkManager = io.mockk.mockk<androidx.work.WorkManager>(relaxed = true)
        io.mockk.every { androidx.work.WorkManager.getInstance(any()) } returns mockWorkManager
    }

    @org.junit.After
    fun teardown() {
        io.mockk.unmockkObject(androidx.work.WorkManager.Companion)
        io.mockk.unmockkStatic(androidx.work.WorkManager::class)
    }

    @Test
    fun restoreBackup_successfulPath_createsAndClearsCheckpoint() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        try {
            val fileProcessor = FakeSettingsFileProcessor(readBytesResult = byteArrayOf(1, 2, 3))
            val tox = FakeToxBackup()
            val toxStarter = FakeToxStarter(startToxStatus = ToxSaveStatus.Ok)

            val importBackupUseCase = FakeImportBackupUseCase()
            val getBackupProviderDataUseCase = FakeGetBackupProviderDataUseCase(providerDataResult = byteArrayOf(4, 5, 6))
            val profileDeleter = FakeProfileDeleter(createCheckpointResult = true)
            val userManager = FakeUserManager(tox)

            val viewModel = BackupSettingsViewModel(
                context = io.mockk.mockk(relaxed = true),
                getUserSettingsUseCase = io.mockk.mockk(relaxed = true),
                fileProcessor = fileProcessor,
                startToxUseCase = StartToxUseCase(toxStarter),
                manageToxLifecycleUseCase = ManageToxLifecycleUseCase(tox, toxStarter),
                getToxRunningStateUseCase = GetToxRunningStateUseCase(tox),
                importBackupUseCase = importBackupUseCase,
                getBackupProviderDataUseCase = getBackupProviderDataUseCase,
                backupProviders = emptyList(),
                clearDatabaseUseCase = ClearDatabaseUseCase(profileDeleter),
                getSelfUserUseCase = GetSelfUserUseCase(userManager, tox, profileDeleter),
                verifyProfileExistsUseCase = VerifyProfileExistsUseCase(userManager),
                manageProfileCheckpointUseCase = ManageProfileCheckpointUseCase(profileDeleter),
                getCloudBackupsUseCase = io.mockk.mockk(relaxed = true),
                downloadCloudBackupUseCase = io.mockk.mockk(relaxed = true),
                ioDispatcher = testDispatcher
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
            while (!profileDeleter.clearCheckpointCalled && attempts < 50) {
                Thread.sleep(50)
                attempts++
            }
            Thread.sleep(50)

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
            val tox = FakeToxBackup()
            val toxStarter = FakeToxStarter()

            val importBackupUseCase = FakeImportBackupUseCase()
            val getBackupProviderDataUseCase = FakeGetBackupProviderDataUseCase(null)
            val profileDeleter = FakeProfileDeleter(createCheckpointResult = true)
            val userManager = FakeUserManager(tox)

            val viewModel = BackupSettingsViewModel(
                context = io.mockk.mockk(relaxed = true),
                getUserSettingsUseCase = io.mockk.mockk(relaxed = true),
                fileProcessor = fileProcessor,
                startToxUseCase = StartToxUseCase(toxStarter),
                manageToxLifecycleUseCase = ManageToxLifecycleUseCase(tox, toxStarter),
                getToxRunningStateUseCase = GetToxRunningStateUseCase(tox),
                importBackupUseCase = importBackupUseCase,
                getBackupProviderDataUseCase = getBackupProviderDataUseCase,
                backupProviders = emptyList(),
                clearDatabaseUseCase = ClearDatabaseUseCase(profileDeleter),
                getSelfUserUseCase = GetSelfUserUseCase(userManager, tox, profileDeleter),
                verifyProfileExistsUseCase = VerifyProfileExistsUseCase(userManager),
                manageProfileCheckpointUseCase = ManageProfileCheckpointUseCase(profileDeleter),
                getCloudBackupsUseCase = io.mockk.mockk(relaxed = true),
                downloadCloudBackupUseCase = io.mockk.mockk(relaxed = true),
                ioDispatcher = testDispatcher
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
            while (!profileDeleter.restoreFromCheckpointCalled && attempts < 50) {
                Thread.sleep(50)
                attempts++
            }
            Thread.sleep(50)

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
            val tox = FakeToxBackup()
            val toxStarter = FakeToxStarter(startToxStatus = ToxSaveStatus.Encrypted)

            val importBackupUseCase = FakeImportBackupUseCase()
            val getBackupProviderDataUseCase = FakeGetBackupProviderDataUseCase(providerDataResult = byteArrayOf(4, 5, 6))
            val profileDeleter = FakeProfileDeleter(createCheckpointResult = true)
            val userManager = FakeUserManager(tox)

            val viewModel = BackupSettingsViewModel(
                context = io.mockk.mockk(relaxed = true),
                getUserSettingsUseCase = io.mockk.mockk(relaxed = true),
                fileProcessor = fileProcessor,
                startToxUseCase = StartToxUseCase(toxStarter),
                manageToxLifecycleUseCase = ManageToxLifecycleUseCase(tox, toxStarter),
                getToxRunningStateUseCase = GetToxRunningStateUseCase(tox),
                importBackupUseCase = importBackupUseCase,
                getBackupProviderDataUseCase = getBackupProviderDataUseCase,
                backupProviders = emptyList(),
                clearDatabaseUseCase = ClearDatabaseUseCase(profileDeleter),
                getSelfUserUseCase = GetSelfUserUseCase(userManager, tox, profileDeleter),
                verifyProfileExistsUseCase = VerifyProfileExistsUseCase(userManager),
                manageProfileCheckpointUseCase = ManageProfileCheckpointUseCase(profileDeleter),
                getCloudBackupsUseCase = io.mockk.mockk(relaxed = true),
                downloadCloudBackupUseCase = io.mockk.mockk(relaxed = true),
                ioDispatcher = testDispatcher
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
            while (!profileDeleter.restoreFromCheckpointCalled && attempts < 50) {
                Thread.sleep(50)
                attempts++
            }
            Thread.sleep(50)

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
    ) : IProfileRepository {
        var createCheckpointCalled = false
        var restoreFromCheckpointCalled = false
        var clearCheckpointCalled = false
        var clearDatabaseCalled = false

        override suspend fun deleteProfile(publicKey: PublicKey) {}

        override suspend fun clearDatabase() {
            clearDatabaseCalled = true
        }

        override suspend fun createCheckpoint(): Boolean {
            clearCheckpointCalled = false
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

        override suspend fun finalizeProfileCreation(oldId: String, newId: String, name: String) {}

        override fun getActiveProfileId(): String = "default"
        override fun setActiveProfileId(id: String) {}
        override fun getShowProfilePicker(): Boolean = false
        override fun setShowProfilePicker(show: Boolean) {}
        override fun getProfiles(): List<ProfileInfo> = emptyList()
        override fun addOrUpdateProfile(profile: ProfileInfo) {}
        override fun removeProfile(id: String) {}
    }

    private class FakeToxBackup : FakeTox(publicKey = PublicKey("fake_public_key")) {
        override fun getName(): String = "Fake User"
        override fun getStatusMessage(): String = "Fake Status"
        override fun getStatus(): UserStatus = UserStatus.Away
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



    private inner class FakeImportBackupUseCase : ImportBackupUseCase(emptyList(), fakePlatformServices) {
        override suspend fun execute(data: ByteArray, skipIds: Set<String>) {}
    }

    private inner class FakeGetBackupProviderDataUseCase(
        private val providerDataResult: ByteArray? = null
    ) : GetBackupProviderDataUseCase(fakePlatformServices) {
        override suspend fun execute(data: ByteArray, id: String): ByteArray? {
            return providerDataResult
        }
    }

    private class FakeUserRepository : IUserRepository {
        override fun get(publicKey: String): kotlinx.coroutines.flow.Flow<User?> = kotlinx.coroutines.flow.emptyFlow()
        override suspend fun add(user: User) {}
        override suspend fun exists(publicKey: String): Boolean = false
        override suspend fun updateName(publicKey: String, name: String) {}
        override suspend fun updateStatusMessage(publicKey: String, statusMessage: String) {}
        override suspend fun updateStatus(publicKey: String, status: UserStatus) {}
        override suspend fun update(user: User) {}
        override suspend fun updateConnection(publicKey: String, connectionStatus: ConnectionStatus) {}
    }

    private class FakeUserManager(tox: ITox) : UserManager(
        userRepository = FakeUserRepository(),
        tox = tox
    ) {
        override suspend fun verifyExists(publicKey: PublicKey): Result<Unit> {
            return Result.success(Unit)
        }
    }
}
