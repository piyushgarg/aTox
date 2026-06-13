package ltd.evilcorp.core.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.FtAutoAccept
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import ltd.evilcorp.domain.features.settings.model.UserSettings
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class UserSettingsRepositoryImplTest {

    private lateinit var repository: UserSettingsRepositoryImpl

    @BeforeTest
    fun setUp() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = UserSettingsRepositoryImpl(context, this)
    }

    private suspend fun awaitSetting(predicate: (UserSettings) -> Boolean): UserSettings {
        return repository.settings.first(predicate)
    }

    @Test
    fun testUpdateThemeMode() = runTest {
        repository.updateThemeMode(2)
        val settings = awaitSetting { it.themeMode == 2 }
        assertEquals(2, settings.themeMode)
    }

    @Test
    fun testUpdateDynamicColorEnabled() = runTest {
        repository.updateDynamicColorEnabled(false)
        val settings = awaitSetting { !it.dynamicColorEnabled }
        assertFalse(settings.dynamicColorEnabled)
        
        repository.updateDynamicColorEnabled(true)
        val settingsTrue = awaitSetting { it.dynamicColorEnabled }
        assertTrue(settingsTrue.dynamicColorEnabled)
    }

    @Test
    fun testUpdateAccentColorSeed() = runTest {
        repository.updateAccentColorSeed(0xFF00FF00.toInt())
        val settings = awaitSetting { it.accentColorSeed == 0xFF00FF00.toInt() }
        assertEquals(0xFF00FF00.toInt(), settings.accentColorSeed)
    }

    @Test
    fun testUpdateLocaleTag() = runTest {
        repository.updateLocaleTag("sv-SE")
        val settings = awaitSetting { it.localeTag == "sv-SE" }
        assertEquals("sv-SE", settings.localeTag)
    }

    @Test
    fun testUpdateDateFormatPreference() = runTest {
        repository.updateDateFormatPreference(DateFormatPreference.YMD)
        val settings = awaitSetting { it.dateFormatPreference == DateFormatPreference.YMD }
        assertEquals(DateFormatPreference.YMD, settings.dateFormatPreference)
    }

    @Test
    fun testUpdateTimeFormatPreference() = runTest {
        repository.updateTimeFormatPreference(TimeFormatPreference.Hours12)
        val settings = awaitSetting { it.timeFormatPreference == TimeFormatPreference.Hours12 }
        assertEquals(TimeFormatPreference.Hours12, settings.timeFormatPreference)
    }

    @Test
    fun testUpdateUdpEnabled() = runTest {
        repository.updateUdpEnabled(true)
        val settings = awaitSetting { it.udpEnabled }
        assertTrue(settings.udpEnabled)
    }

    @Test
    fun testUpdateRunAtStartup() = runTest {
        repository.updateRunAtStartup(true)
        val settings = awaitSetting { it.runAtStartup }
        assertTrue(settings.runAtStartup)
    }

    @Test
    fun testUpdateAutoAwayEnabled() = runTest {
        repository.updateAutoAwayEnabled(true)
        val settings = awaitSetting { it.autoAwayEnabled }
        assertTrue(settings.autoAwayEnabled)
    }

    @Test
    fun testUpdateAutoAwaySeconds() = runTest {
        repository.updateAutoAwaySeconds(300L)
        val settings = awaitSetting { it.autoAwaySeconds == 300L }
        assertEquals(300L, settings.autoAwaySeconds)
    }

    @Test
    fun testUpdateProxyType() = runTest {
        repository.updateProxyType(ProxyType.SOCKS5)
        val settings = awaitSetting { it.proxyType == ProxyType.SOCKS5 }
        assertEquals(ProxyType.SOCKS5, settings.proxyType)
    }

    @Test
    fun testUpdateProxyAddress() = runTest {
        repository.updateProxyAddress("127.0.0.1")
        val settings = awaitSetting { it.proxyAddress == "127.0.0.1" }
        assertEquals("127.0.0.1", settings.proxyAddress)
    }

    @Test
    fun testUpdateProxyPort() = runTest {
        repository.updateProxyPort(9050)
        val settings = awaitSetting { it.proxyPort == 9050 }
        assertEquals(9050, settings.proxyPort)
    }

    @Test
    fun testUpdateFtAutoAccept() = runTest {
        repository.updateFtAutoAccept(FtAutoAccept.All)
        val settings = awaitSetting { it.ftAutoAccept == FtAutoAccept.All }
        assertEquals(FtAutoAccept.All, settings.ftAutoAccept)
    }

    @Test
    fun testUpdateBootstrapNodeSource() = runTest {
        repository.updateBootstrapNodeSource(BootstrapNodeSource.UserProvided)
        val settings = awaitSetting { it.bootstrapNodeSource == BootstrapNodeSource.UserProvided }
        assertEquals(BootstrapNodeSource.UserProvided, settings.bootstrapNodeSource)
    }

    @Test
    fun testUpdateDisableScreenshots() = runTest {
        repository.updateDisableScreenshots(true)
        val settings = awaitSetting { it.disableScreenshots }
        assertTrue(settings.disableScreenshots)
    }

    @Test
    fun testUpdateConfirmQuitting() = runTest {
        repository.updateConfirmQuitting(false)
        val settings = awaitSetting { !it.confirmQuitting }
        assertFalse(settings.confirmQuitting)
    }

    @Test
    fun testUpdateConfirmCalling() = runTest {
        repository.updateConfirmCalling(false)
        val settings = awaitSetting { !it.confirmCalling }
        assertFalse(settings.confirmCalling)
    }

    @Test
    fun testUpdateSentMessageSoundVolume() = runTest {
        repository.updateSentMessageSoundVolume(50)
        val settings = awaitSetting { it.sentMessageSoundVolume == 50 }
        assertEquals(50, settings.sentMessageSoundVolume)
    }

    @Test
    fun testUpdateSentMessageSoundUri() = runTest {
        repository.updateSentMessageSoundUri("content://sound")
        val settings = awaitSetting { it.sentMessageSoundUri == "content://sound" }
        assertEquals("content://sound", settings.sentMessageSoundUri)
    }

    @Test
    fun testUpdateHapticEnabled() = runTest {
        repository.updateHapticEnabled(false)
        val settings = awaitSetting { !it.hapticEnabled }
        assertFalse(settings.hapticEnabled)
    }

    @Test
    fun testUpdateAutomaticBackupEnabled() = runTest {
        repository.updateAutomaticBackupEnabled(true)
        val settings = awaitSetting { it.automaticBackupEnabled }
        assertTrue(settings.automaticBackupEnabled)
    }

    @Test
    fun testUpdateBackupFrequency() = runTest {
        repository.updateBackupFrequency(BackupFrequency.Daily)
        val settings = awaitSetting { it.backupFrequency == BackupFrequency.Daily }
        assertEquals(BackupFrequency.Daily, settings.backupFrequency)
    }

    @Test
    fun testUpdateBackupGoogleAccount() = runTest {
        repository.updateBackupGoogleAccount("test@gmail.com")
        val settings = awaitSetting { it.backupGoogleAccount == "test@gmail.com" }
        assertEquals("test@gmail.com", settings.backupGoogleAccount)
    }

    @Test
    fun testUpdateBackupUseCellular() = runTest {
        repository.updateBackupUseCellular(true)
        val settings = awaitSetting { it.backupUseCellular }
        assertTrue(settings.backupUseCellular)
    }
}
