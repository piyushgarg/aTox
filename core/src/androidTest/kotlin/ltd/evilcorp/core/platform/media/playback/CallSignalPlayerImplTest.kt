// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.media.playback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.features.settings.model.AppSound
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.FtAutoAccept
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CallSignalPlayerImplTest {

    private lateinit var fakeUserSettingsRepository: FakeUserSettingsRepository
    private lateinit var player: CallSignalPlayerImpl

    private class FakeUserSettingsRepository(initialSettings: UserSettings = UserSettings()) : IUserSettingsRepository {
        private val _settings = MutableStateFlow(initialSettings)
        override val settings: StateFlow<UserSettings> = _settings.asStateFlow()

        override suspend fun updateThemeMode(themeMode: Int) {}
        override suspend fun updateDynamicColorEnabled(enabled: Boolean) {}
        override suspend fun updateAccentColorSeed(accentColorSeed: Int) {}
        override suspend fun updateLocaleTag(localeTag: String) {}
        override suspend fun updateDateFormatPreference(preference: DateFormatPreference) {}
        override suspend fun updateTimeFormatPreference(preference: TimeFormatPreference) {}
        override suspend fun updateUdpEnabled(enabled: Boolean) {}
        override suspend fun updateRunAtStartup(enabled: Boolean) {}
        override suspend fun updateAutoAwayEnabled(enabled: Boolean) {}
        override suspend fun updateAutoAwaySeconds(seconds: Long) {}
        override suspend fun updateProxyType(type: ProxyType) {}
        override suspend fun updateProxyAddress(address: String) {}
        override suspend fun updateProxyPort(port: Int) {}
        override suspend fun updateFtAutoAccept(value: FtAutoAccept) {}
        override suspend fun updateBootstrapNodeSource(value: BootstrapNodeSource) {}
        override suspend fun updateDisableScreenshots(disable: Boolean) {}
        override suspend fun updateConfirmQuitting(confirm: Boolean) {}
        override suspend fun updateConfirmCalling(confirm: Boolean) {}
        override suspend fun updateEnableReplies(enabled: Boolean) {}
        override suspend fun updateSentMessageSoundVolume(volume: Int) {}
        override suspend fun updateSentMessageSoundUri(uri: String) {}
        override suspend fun updateCallSound(sound: AppSound) {}
        override suspend fun updateCallSoundVolume(volume: Int) {
            _settings.value = _settings.value.copy(callSoundVolume = volume)
        }
        override suspend fun updateCallRingtoneUri(uri: String) {
            _settings.value = _settings.value.copy(callRingtoneUri = uri)
        }
        override suspend fun updateNotificationSoundVolume(volume: Int) {}
        override suspend fun updateNotificationSoundUri(uri: String) {}
        override suspend fun updateActiveChatSoundVolume(volume: Int) {}
        override suspend fun updateActiveChatSoundUri(uri: String) {}
        override suspend fun updateHapticEnabled(enabled: Boolean) {}
        override suspend fun updateAutoSaveToDownloads(enabled: Boolean) {}
        override suspend fun updateAutoSaveDirectoryUri(uri: String) {}
        override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) {}
        override suspend fun updateBackupFrequency(frequency: BackupFrequency) {}
        override suspend fun updateBackupGoogleAccount(account: String) {}
        override suspend fun updateBackupUseCellular(enabled: Boolean) {}
        override suspend fun updateBackupDestinationOrdinals(ordinals: Set<Int>) {}
        override suspend fun updateLastLocalBackupTimeMs(timeMs: Long) {}
        override suspend fun updateLastLocalBackupSizeKb(sizeKb: Long) {}
        override suspend fun updateLastGoogleBackupTimeMs(timeMs: Long) {}
        override suspend fun updateLastGoogleBackupSizeKb(sizeKb: Long) {}
    }

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fakeUserSettingsRepository = FakeUserSettingsRepository()
        player = CallSignalPlayerImpl(context, fakeUserSettingsRepository)
    }

    @AfterTest
    fun tearDown() {
        player.stopSignals()
    }

    @Test
    fun testPlayIncomingRingtone() = runTest {
        // Play ringtone with default preferences
        player.playIncomingRingtone(this)
        
        // Wait a short time to let the coroutine initiate playback
        kotlinx.coroutines.delay(300)
        
        // Stop playback
        player.stopSignals()
    }

    @Test
    fun testPlayRingback() = runTest {
        val callActive = AtomicBoolean(true)
        
        // Play ringback tone generator
        player.playRingback(this, isCallActive = { callActive.get() })
        
        // Wait a brief moment to let ToneGenerator trigger
        kotlinx.coroutines.delay(200)

        // Set call active to false to naturally exit loop and stop signals
        callActive.set(false)
        player.stopSignals()
    }
}
