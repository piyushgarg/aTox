package ltd.evilcorp.domain.features.settings.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.network.bootstrap.IBootstrapNodeRegistry
import ltd.evilcorp.domain.core.network.bootstrap.BootstrapNode
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.transfer.FileStoragePlatformCoordinator
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.FileTransferRepositories
import ltd.evilcorp.domain.fakes.*
import kotlin.test.*

class SettingsUseCasesTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val settingsRepo = FakeUserSettingsRepository()
    
    private val nodeRegistry = object : IBootstrapNodeRegistry {
        var resetCalled = false
        override suspend fun get(n: Int): List<BootstrapNode> = emptyList()
        override suspend fun reset() {
            resetCalled = true
        }
    }

    private val platformHelper = FakeFileTransferPlatformHelper()
    private val storageHelper = FakeFileStorageHelper()
    private val storageCoordinator = FileStoragePlatformCoordinator(platformHelper, storageHelper)

    private val contactRepo = FakeContactRepository()
    private val messageRepo = FakeMessageRepository()
    private val transferRepo = FakeFileTransferRepository()
    private val repositories = FileTransferRepositories(contactRepo, messageRepo, transferRepo, settingsRepo)

    private val sessionRegistry = FakeFileTransferSessionRegistry()
    private val tox = FakeTox()

    private val fileTransferManager = FileTransferManager(
        scope,
        storageCoordinator,
        repositories,
        tox,
        tox,
        sessionRegistry,
        Dispatchers.Unconfined
    )

    @Test
    fun `GetUserSettingsUseCase returns settings state flow`() = runTest {
        val useCase = GetUserSettingsUseCase(settingsRepo)
        val initialSettings = useCase.settings.value
        assertEquals(UserSettings(), initialSettings)

        settingsRepo.updateUdpEnabled(true)
        assertTrue(useCase.settings.value.udpEnabled)
    }

    @Test
    fun `UpdateUserSettingsUseCase handles various update actions`() = runTest {
        val useCase = UpdateUserSettingsUseCase(settingsRepo, nodeRegistry)

        useCase.execute(UpdateAction.UdpEnabled(true))
        assertTrue(settingsRepo.settings.value.udpEnabled)

        useCase.execute(UpdateAction.ProxyPort(8080))
        assertEquals(8080, settingsRepo.settings.value.proxyPort)

        useCase.execute(UpdateAction.BootstrapNodeSourceAction(BootstrapNodeSource.UserProvided))
        assertEquals(BootstrapNodeSource.UserProvided, settingsRepo.settings.value.bootstrapNodeSource)
        assertTrue(nodeRegistry.resetCalled)

        useCase.execute(UpdateAction.BackupFrequencyAction(BackupFrequency.Weekly))
        assertEquals(BackupFrequency.Weekly, settingsRepo.settings.value.backupFrequency)

        useCase.execute(UpdateAction.BackupDestinationOrdinals(setOf(1, 2)))
        assertEquals(setOf(1, 2), settingsRepo.settings.value.backupDestinationOrdinals)

        useCase.execute(UpdateAction.SentMessageSoundUri("uri://sent"))
        assertEquals("uri://sent", settingsRepo.settings.value.sentMessageSoundUri)

        useCase.execute(UpdateAction.CallRingtoneUri("uri://ringtone"))
        assertEquals("uri://ringtone", settingsRepo.settings.value.callRingtoneUri)

        useCase.execute(UpdateAction.NotificationSoundUri("uri://notification"))
        assertEquals("uri://notification", settingsRepo.settings.value.notificationSoundUri)

        useCase.execute(UpdateAction.ActiveChatSoundUri("uri://chat"))
        assertEquals("uri://chat", settingsRepo.settings.value.activeChatSoundUri)

        useCase.execute(UpdateAction.AutoSaveDirectoryUri("uri://dir"))
        assertEquals("uri://dir", settingsRepo.settings.value.autoSaveDirectoryUri)
    }

    @Test
    fun `ClearCacheUseCase delegates to fileTransferManager`() {
        val useCase = ClearCacheUseCase(fileTransferManager)
        
        storageHelper.clearCacheResult = true
        useCase.execute()
        // No exception indicates successful integration
    }

    @Test
    fun `GetCacheSizeUseCase delegates to fileTransferManager`() {
        val useCase = GetCacheSizeUseCase(fileTransferManager)
        
        storageHelper.cacheSizeVal = 2048L
        val size = useCase.execute()
        
        assertEquals(2048L, size)
    }
}
