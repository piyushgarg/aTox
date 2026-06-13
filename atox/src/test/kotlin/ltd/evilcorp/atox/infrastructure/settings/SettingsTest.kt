package ltd.evilcorp.atox.infrastructure.settings

import android.content.Context
import android.content.pm.PackageManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.features.settings.model.*
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockRepository = mockk<IUserSettingsRepository>(relaxed = true)
    private val settingsFlow = MutableStateFlow(UserSettings())

    init {
        every { mockRepository.settings } returns settingsFlow
    }

    private fun TestScope.createSettings(): Settings {
        return Settings(mockContext, mockRepository, this)
    }

    @Test
    fun testUdpEnabled() = runTest {
        val settings = createSettings()
        
        settingsFlow.value = UserSettings(udpEnabled = true)
        assertTrue(settings.udpEnabled)
        
        settings.udpEnabled = false
        runCurrent()
        coVerify { mockRepository.updateUdpEnabled(false) }
    }

    @Test
    fun testRunAtStartup() = runTest {
        val mockPm = mockk<PackageManager>(relaxed = true)
        every { mockContext.packageManager } returns mockPm
        every { mockPm.getComponentEnabledSetting(any()) } returns PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        val settings = createSettings()
        
        settingsFlow.value = UserSettings(runAtStartup = true)
        assertTrue(settings.runAtStartup)
        
        settings.runAtStartup = true
        runCurrent()
        coVerify { mockRepository.updateRunAtStartup(true) }
        coVerify { mockPm.setComponentEnabledSetting(any(), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, any()) }
    }

    @Test
    fun testAutoAway() = runTest {
        val settings = createSettings()
        
        settingsFlow.value = UserSettings(autoAwayEnabled = true, autoAwaySeconds = 120L)
        assertTrue(settings.autoAwayEnabled)
        assertEquals(120L, settings.autoAwaySeconds)
        
        settings.autoAwayEnabled = false
        settings.autoAwaySeconds = 300L
        runCurrent()
        coVerify { mockRepository.updateAutoAwayEnabled(false) }
        coVerify { mockRepository.updateAutoAwaySeconds(300L) }
    }

    @Test
    fun testProxy() = runTest {
        val settings = createSettings()
        
        settingsFlow.value = UserSettings(
            proxyType = ProxyType.SOCKS5,
            proxyAddress = "127.0.0.1",
            proxyPort = 9050
        )
        assertEquals(ProxyType.SOCKS5, settings.proxyType)
        assertEquals("127.0.0.1", settings.proxyAddress)
        assertEquals(9050, settings.proxyPort)
        
        settings.proxyType = ProxyType.HTTP
        settings.proxyAddress = "localhost"
        settings.proxyPort = 8080
        runCurrent()
        coVerify { mockRepository.updateProxyType(ProxyType.HTTP) }
        coVerify { mockRepository.updateProxyAddress("localhost") }
        coVerify { mockRepository.updateProxyPort(8080) }
    }

    @Test
    fun testDateTimeFormats() = runTest {
        val settings = createSettings()
        
        settingsFlow.value = UserSettings(
            dateFormatPreference = DateFormatPreference.YMD,
            timeFormatPreference = TimeFormatPreference.Hours24
        )
        assertEquals(DateFormatPreference.YMD, settings.dateFormatPreference)
        assertEquals(TimeFormatPreference.Hours24, settings.timeFormatPreference)
        
        settings.dateFormatPreference = DateFormatPreference.DMY
        settings.timeFormatPreference = TimeFormatPreference.Hours12
        runCurrent()
        coVerify { mockRepository.updateDateFormatPreference(DateFormatPreference.DMY) }
        coVerify { mockRepository.updateTimeFormatPreference(TimeFormatPreference.Hours12) }
    }

    @Test
    fun testFtAutoAcceptAndSave() = runTest {
        val settings = createSettings()
        
        settingsFlow.value = UserSettings(
            ftAutoAccept = FtAutoAccept.All,
            autoSaveToDownloads = true,
            autoSaveDirectoryUri = "content://directory"
        )
        assertEquals(FtAutoAccept.All, settings.ftAutoAccept)
        assertTrue(settings.autoSaveToDownloads)
        assertEquals("content://directory", settings.autoSaveDirectoryUri)
        
        settings.ftAutoAccept = FtAutoAccept.Images
        settings.autoSaveToDownloads = false
        settings.autoSaveDirectoryUri = "content://other"
        runCurrent()
        coVerify { mockRepository.updateFtAutoAccept(FtAutoAccept.Images) }
        coVerify { mockRepository.updateAutoSaveToDownloads(false) }
        coVerify { mockRepository.updateAutoSaveDirectoryUri("content://other") }
    }

    @Test
    fun testBootstrapNodes() = runTest {
        val settings = createSettings()
        
        settingsFlow.value = UserSettings(bootstrapNodeSource = BootstrapNodeSource.UserProvided)
        assertEquals(BootstrapNodeSource.UserProvided, settings.bootstrapNodeSource)
        
        settings.bootstrapNodeSource = BootstrapNodeSource.BuiltIn
        runCurrent()
        coVerify { mockRepository.updateBootstrapNodeSource(BootstrapNodeSource.BuiltIn) }
    }

    @Test
    fun testPrivacyAndConfirmation() = runTest {
        val settings = createSettings()
        
        settingsFlow.value = UserSettings(
            disableScreenshots = true,
            confirmQuitting = false,
            confirmCalling = false
        )
        assertTrue(settings.disableScreenshots)
        assertTrue(!settings.confirmQuitting)
        assertTrue(!settings.confirmCalling)
        
        settings.disableScreenshots = false
        settings.confirmQuitting = true
        settings.confirmCalling = true
        runCurrent()
        coVerify { mockRepository.updateDisableScreenshots(false) }
        coVerify { mockRepository.updateConfirmQuitting(true) }
        coVerify { mockRepository.updateConfirmCalling(true) }
    }

    @Test
    fun testRepliesAndHaptic() = runTest {
        val settings = createSettings()
        
        settingsFlow.value = UserSettings(enableReplies = false, hapticEnabled = false)
        assertTrue(!settings.enableReplies)
        assertTrue(!settings.hapticEnabled)
        
        settings.enableReplies = true
        settings.hapticEnabled = true
        runCurrent()
        coVerify { mockRepository.updateEnableReplies(true) }
        coVerify { mockRepository.updateHapticEnabled(true) }
    }

    @Test
    fun testSoundSettings() = runTest {
        val settings = createSettings()
        
        settingsFlow.value = UserSettings(
            sentMessageSoundVolume = 30,
            sentMessageSoundUri = "content://sent",
            callSound = AppSound.SoftPop,
            callSoundVolume = 80,
            callRingtoneUri = "content://ring",
            notificationSoundVolume = 60,
            notificationSoundUri = "content://notify",
            activeChatSoundVolume = 40,
            activeChatSoundUri = "content://chat"
        )
        
        assertEquals(30, settings.sentMessageSoundVolume)
        assertEquals("content://sent", settings.sentMessageSoundUri)
        assertEquals(AppSound.SoftPop, settings.callSound)
        assertEquals(80, settings.callSoundVolume)
        assertEquals("content://ring", settings.callRingtoneUri)
        assertEquals(60, settings.notificationSoundVolume)
        assertEquals("content://notify", settings.notificationSoundUri)
        assertEquals(40, settings.activeChatSoundVolume)
        assertEquals("content://chat", settings.activeChatSoundUri)
        
        settings.sentMessageSoundVolume = 50
        settings.sentMessageSoundUri = "content://sent2"
        settings.callSound = AppSound.Pulse
        settings.callSoundVolume = 90
        settings.callRingtoneUri = "content://ring2"
        settings.notificationSoundVolume = 70
        settings.notificationSoundUri = "content://notify2"
        settings.activeChatSoundVolume = 50
        settings.activeChatSoundUri = "content://chat2"
        runCurrent()
        
        coVerify { mockRepository.updateSentMessageSoundVolume(50) }
        coVerify { mockRepository.updateSentMessageSoundUri("content://sent2") }
        coVerify { mockRepository.updateCallSound(AppSound.Pulse) }
        coVerify { mockRepository.updateCallSoundVolume(90) }
        coVerify { mockRepository.updateCallRingtoneUri("content://ring2") }
        coVerify { mockRepository.updateNotificationSoundVolume(70) }
        coVerify { mockRepository.updateNotificationSoundUri("content://notify2") }
        coVerify { mockRepository.updateActiveChatSoundVolume(50) }
        coVerify { mockRepository.updateActiveChatSoundUri("content://chat2") }
    }

    @Test
    fun testBackupSettings() = runTest {
        val settings = createSettings()
        
        settingsFlow.value = UserSettings(
            automaticBackupEnabled = true,
            backupFrequency = BackupFrequency.Weekly,
            backupGoogleAccount = "user@mail.com",
            backupUseCellular = true,
            lastLocalBackupTimeMs = 1000L,
            lastLocalBackupSizeKb = 200L,
            lastGoogleBackupTimeMs = 3000L,
            lastGoogleBackupSizeKb = 400L,
            backupDestinationOrdinals = setOf(BackupDestination.Local.ordinal, BackupDestination.GoogleDrive.ordinal)
        )
        
        assertTrue(settings.automaticBackupEnabled)
        assertEquals(BackupFrequency.Weekly, settings.backupFrequency)
        assertEquals("user@mail.com", settings.backupGoogleAccount)
        assertTrue(settings.backupUseCellular)
        assertEquals(1000L, settings.lastLocalBackupTimeMs)
        assertEquals(200L, settings.lastLocalBackupSizeKb)
        assertEquals(3000L, settings.lastGoogleBackupTimeMs)
        assertEquals(400L, settings.lastGoogleBackupSizeKb)
        assertEquals(setOf(BackupDestination.Local, BackupDestination.GoogleDrive), settings.backupDestinations)
        
        settings.automaticBackupEnabled = false
        settings.backupFrequency = BackupFrequency.Daily
        settings.backupGoogleAccount = "other@mail.com"
        settings.backupUseCellular = false
        settings.lastLocalBackupTimeMs = 2000L
        settings.lastLocalBackupSizeKb = 300L
        settings.lastGoogleBackupTimeMs = 4000L
        settings.lastGoogleBackupSizeKb = 500L
        settings.backupDestinations = setOf(BackupDestination.Local)
        runCurrent()
        
        coVerify { mockRepository.updateAutomaticBackupEnabled(false) }
        coVerify { mockRepository.updateBackupFrequency(BackupFrequency.Daily) }
        coVerify { mockRepository.updateBackupGoogleAccount("other@mail.com") }
        coVerify { mockRepository.updateBackupUseCellular(false) }
        coVerify { mockRepository.updateLastLocalBackupTimeMs(2000L) }
        coVerify { mockRepository.updateLastLocalBackupSizeKb(300L) }
        coVerify { mockRepository.updateLastGoogleBackupTimeMs(4000L) }
        coVerify { mockRepository.updateLastGoogleBackupSizeKb(500L) }
        coVerify { mockRepository.updateBackupDestinationOrdinals(setOf(BackupDestination.Local.ordinal)) }
    }
}
