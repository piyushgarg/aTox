package ltd.evilcorp.atox.ui.settings

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.atox.MainDispatcherRule
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.usecase.CheckProxyUseCase
import ltd.evilcorp.domain.features.settings.usecase.ClearCacheUseCase
import ltd.evilcorp.domain.features.settings.usecase.GetCacheSizeUseCase
import ltd.evilcorp.domain.features.settings.usecase.GetUserSettingsUseCase
import ltd.evilcorp.domain.features.settings.usecase.ManageToxLifecycleUseCase
import ltd.evilcorp.domain.features.settings.usecase.SetRunAtStartupUseCase
import ltd.evilcorp.domain.features.settings.usecase.UpdateAction
import ltd.evilcorp.domain.features.settings.usecase.UpdateUserSettingsUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockGetUserSettingsUseCase = mockk<GetUserSettingsUseCase>()
    private val mockUpdateUserSettingsUseCase = mockk<UpdateUserSettingsUseCase>(relaxed = true)
    private val mockManageToxLifecycleUseCase = mockk<ManageToxLifecycleUseCase>(relaxed = true)
    private val mockGetCacheSizeUseCase = mockk<GetCacheSizeUseCase>()
    private val mockClearCacheUseCase = mockk<ClearCacheUseCase>()
    private val mockSetRunAtStartupUseCase = mockk<SetRunAtStartupUseCase>(relaxed = true)
    private val mockCheckProxyUseCase = mockk<CheckProxyUseCase>()
    private val mockGetSelfUserUseCase = mockk<GetSelfUserUseCase>()

    private fun createViewModel(): SettingsViewModel {
        every { mockGetSelfUserUseCase.publicKey } returns PublicKey("ABCDEF")
        every { mockGetSelfUserUseCase.execute() } returns MutableStateFlow<User?>(null)
        val defaultSettings = UserSettings()
        every { mockGetUserSettingsUseCase.settings } returns MutableStateFlow(defaultSettings)

        return SettingsViewModel(
            mockGetUserSettingsUseCase,
            mockUpdateUserSettingsUseCase,
            mockManageToxLifecycleUseCase,
            mockGetCacheSizeUseCase,
            mockClearCacheUseCase,
            mockSetRunAtStartupUseCase,
            mockCheckProxyUseCase,
            mockGetSelfUserUseCase,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun `setUdpEnabled updates use case and sets restart needed`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.setUdpEnabled(true)
        runCurrent()
        
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.UdpEnabled(true)) }
        
        // Assert restart needed
        assertFalse(viewModel.committed.value)
        
        every { mockManageToxLifecycleUseCase.password } returns "pass"
        every { mockManageToxLifecycleUseCase.started } returns false
        viewModel.commit()
        runCurrent()
        
        coVerify { mockManageToxLifecycleUseCase.execute(ltd.evilcorp.domain.features.settings.usecase.ToxLifecycleAction.Stop) }
        coVerify { mockManageToxLifecycleUseCase.execute(ltd.evilcorp.domain.features.settings.usecase.ToxLifecycleAction.TryLoad("pass")) }
        assertTrue(viewModel.committed.value)
    }

    @Test
    fun `setRunAtStartup calls use case`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRunAtStartup(true)
        runCurrent()
        coVerify { mockSetRunAtStartupUseCase.execute(true) }
    }

    @Test
    fun `commit when restart not needed completes immediately`() = runTest {
        val viewModel = createViewModel()
        viewModel.commit()
        runCurrent()
        assertTrue(viewModel.committed.value)
        coVerify(exactly = 0) { mockManageToxLifecycleUseCase.execute(any()) }
    }

    @Test
    fun `setBackupFrequency calls use case`() = runTest {
        val viewModel = createViewModel()
        val freq = ltd.evilcorp.domain.features.settings.model.BackupFrequency.Daily
        viewModel.setBackupFrequency(freq)
        runCurrent()
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.BackupFrequencyAction(freq)) }
    }

    @Test
    fun `setBackupDestinations calls use case`() = runTest {
        val viewModel = createViewModel()
        val dests = setOf(ltd.evilcorp.domain.features.settings.model.BackupDestination.Local)
        viewModel.setBackupDestinations(dests)
        runCurrent()
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.BackupDestinationOrdinals(setOf(0))) }
    }

    @Test
    fun `setProxyType updates settings and sets restart needed`() = runTest {
        val viewModel = createViewModel()
        val type = ltd.evilcorp.domain.features.settings.model.ProxyType.SOCKS5
        viewModel.setProxyType(type)
        runCurrent()
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.ProxyTypeAction(type)) }
        
        every { mockManageToxLifecycleUseCase.password } returns "pass"
        every { mockManageToxLifecycleUseCase.started } returns false
        viewModel.commit()
        runCurrent()
        coVerify { mockManageToxLifecycleUseCase.execute(ltd.evilcorp.domain.features.settings.usecase.ToxLifecycleAction.Stop) }
    }

    @Test
    fun `setProxyAddress updates settings and sets restart needed if proxy type is not None`() = runTest {
        val viewModel = createViewModel()
        viewModel.setProxyAddress("1.2.3.4")
        runCurrent()
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.ProxyAddressAction("1.2.3.4")) }
        
        val viewModel2 = createViewModel()
        val customSettings = UserSettings(proxyType = ltd.evilcorp.domain.features.settings.model.ProxyType.HTTP)
        every { mockGetUserSettingsUseCase.settings } returns MutableStateFlow(customSettings)
        
        viewModel2.setProxyAddress("5.6.7.8")
        runCurrent()
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.ProxyAddressAction("5.6.7.8")) }
        
        every { mockManageToxLifecycleUseCase.password } returns "pass"
        every { mockManageToxLifecycleUseCase.started } returns false
        viewModel2.commit()
        runCurrent()
        coVerify { mockManageToxLifecycleUseCase.execute(ltd.evilcorp.domain.features.settings.usecase.ToxLifecycleAction.Stop) }
    }

    @Test
    fun `setProxyPortString empty port updates to 0`() = runTest {
        coEvery { mockCheckProxyUseCase.execute(any(), any(), any(), any()) } returns mockk()
        val viewModel = createViewModel()
        assertTrue(viewModel.setProxyPortString(""))
        runCurrent()
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.ProxyPort(0)) }
    }

    @Test
    fun `setProxyPortString valid port updates port`() = runTest {
        coEvery { mockCheckProxyUseCase.execute(any(), any(), any(), any()) } returns mockk()
        val viewModel = createViewModel()
        assertTrue(viewModel.setProxyPortString("1080"))
        runCurrent()
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.ProxyPort(1080)) }
    }

    @Test
    fun `setProxyPortString invalid port returns false and does not update`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.setProxyPortString("invalid"))
        runCurrent()
        coVerify(exactly = 0) { mockUpdateUserSettingsUseCase.execute(any()) }
    }

    @Test
    fun `sound URI setters call update settings`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.setSentMessageSoundUri("uri1")
        viewModel.setCallRingtoneUri("uri2")
        viewModel.setNotificationSoundUri("uri3")
        viewModel.setActiveChatSoundUri("uri4")
        runCurrent()
        
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.SentMessageSoundUri("uri1")) }
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.CallRingtoneUri("uri2")) }
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.NotificationSoundUri("uri3")) }
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.ActiveChatSoundUri("uri4")) }
    }

    @Test
    fun `setAutoSaveDirectoryUri calls update settings`() = runTest {
        val viewModel = createViewModel()
        viewModel.setAutoSaveDirectoryUri("dir_uri")
        runCurrent()
        coVerify { mockUpdateUserSettingsUseCase.execute(UpdateAction.AutoSaveDirectoryUri("dir_uri")) }
    }

    @Test
    fun `cache actions delegate to use cases`() = runTest {
        every { mockGetCacheSizeUseCase.execute() } returns 1024L
        every { mockClearCacheUseCase.execute() } returns Unit
        
        val viewModel = createViewModel()
        
        val size = viewModel.getCacheSize()
        coVerify { mockGetCacheSizeUseCase.execute() }
        assertTrue(size == 1024L)
        
        viewModel.clearCache()
        coVerify { mockClearCacheUseCase.execute() }
    }
}
