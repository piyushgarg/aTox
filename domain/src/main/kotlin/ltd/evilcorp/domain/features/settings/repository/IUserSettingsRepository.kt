package ltd.evilcorp.domain.features.settings.repository

import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.settings.model.FtAutoAccept
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.AppSound
import ltd.evilcorp.domain.features.settings.model.BackupFrequency

@Suppress("ComplexInterface")
interface IUserSettingsRepository {
    val settings: StateFlow<UserSettings>

    suspend fun updateThemeMode(themeMode: Int)
    suspend fun updateDynamicColorEnabled(enabled: Boolean)
    suspend fun updateAccentColorSeed(accentColorSeed: Int)
    suspend fun updateLocaleTag(localeTag: String)
    suspend fun updateDateFormatPreference(preference: DateFormatPreference)
    suspend fun updateTimeFormatPreference(preference: TimeFormatPreference)
    suspend fun updateUdpEnabled(enabled: Boolean)
    suspend fun updateRunAtStartup(enabled: Boolean)
    suspend fun updateAutoAwayEnabled(enabled: Boolean)
    suspend fun updateAutoAwaySeconds(seconds: Long)
    suspend fun updateProxyType(type: ProxyType)
    suspend fun updateProxyAddress(address: String)
    suspend fun updateProxyPort(port: Int)
    suspend fun updateFtAutoAccept(value: FtAutoAccept)
    suspend fun updateBootstrapNodeSource(value: BootstrapNodeSource)
    suspend fun updateDisableScreenshots(disable: Boolean)
    suspend fun updateConfirmQuitting(confirm: Boolean)
    suspend fun updateConfirmCalling(confirm: Boolean)
    suspend fun updateEnableReplies(enabled: Boolean)
    suspend fun updateSentMessageSoundVolume(volume: Int)
    suspend fun updateSentMessageSoundUri(uri: String)
    suspend fun updateCallSound(sound: AppSound)
    suspend fun updateCallSoundVolume(volume: Int)
    suspend fun updateCallRingtoneUri(uri: String)
    suspend fun updateNotificationSoundVolume(volume: Int)
    suspend fun updateNotificationSoundUri(uri: String)
    suspend fun updateActiveChatSoundVolume(volume: Int)
    suspend fun updateActiveChatSoundUri(uri: String)
    suspend fun updateHapticEnabled(enabled: Boolean)
    suspend fun updateAutoSaveToDownloads(enabled: Boolean)
    suspend fun updateAutoSaveDirectoryUri(uri: String)
    suspend fun updateAutomaticBackupEnabled(enabled: Boolean)
    suspend fun updateBackupFrequency(frequency: BackupFrequency)
    suspend fun updateBackupGoogleAccount(account: String)
    suspend fun updateBackupUseCellular(enabled: Boolean)
    suspend fun updateBackupDestinationOrdinals(ordinals: Set<Int>)
    suspend fun updateLocalBackupDirectoryUri(uri: String)
    suspend fun updateLastLocalBackupTimeMs(timeMs: Long)
    suspend fun updateLastLocalBackupSizeKb(sizeKb: Long)
    suspend fun updateLastGoogleBackupTimeMs(timeMs: Long)
    suspend fun updateLastGoogleBackupSizeKb(sizeKb: Long)
}
