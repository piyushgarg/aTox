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
import ltd.evilcorp.domain.features.settings.model.BackupDestination

@Suppress("ComplexInterface")
interface IUserSettingsRepository {
    val settings: StateFlow<UserSettings>

    fun updateThemeMode(themeMode: Int)
    fun updateDynamicColorEnabled(enabled: Boolean)
    fun updateAccentColorSeed(accentColorSeed: Int)
    fun updateLocaleTag(localeTag: String)
    fun updateDateFormatPreference(preference: DateFormatPreference)
    fun updateTimeFormatPreference(preference: TimeFormatPreference)
    fun updateUdpEnabled(enabled: Boolean)
    fun updateRunAtStartup(enabled: Boolean)
    fun updateAutoAwayEnabled(enabled: Boolean)
    fun updateAutoAwaySeconds(seconds: Long)
    fun updateProxyType(type: ProxyType)
    fun updateProxyAddress(address: String)
    fun updateProxyPort(port: Int)
    fun updateFtAutoAccept(value: FtAutoAccept)
    fun updateBootstrapNodeSource(value: BootstrapNodeSource)
    fun updateDisableScreenshots(disable: Boolean)
    fun updateConfirmQuitting(confirm: Boolean)
    fun updateConfirmCalling(confirm: Boolean)
    fun updateEnableReplies(enabled: Boolean)
    fun updateSentMessageSoundVolume(volume: Int)
    fun updateSentMessageSoundUri(uri: String)
    fun updateCallSound(sound: AppSound)
    fun updateCallSoundVolume(volume: Int)
    fun updateCallRingtoneUri(uri: String)
    fun updateNotificationSoundVolume(volume: Int)
    fun updateNotificationSoundUri(uri: String)
    fun updateActiveChatSoundVolume(volume: Int)
    fun updateActiveChatSoundUri(uri: String)
    fun updateHapticEnabled(enabled: Boolean)
    fun updateAutoSaveToDownloads(enabled: Boolean)
    fun updateAutoSaveDirectoryUri(uri: String)
    fun updateBackupEncryptionEnabled(enabled: Boolean)
    fun updateBackupEndToEndEncryptionEnabled(enabled: Boolean)
    fun updateAutomaticBackupEnabled(enabled: Boolean)
    fun updateBackupFrequency(frequency: BackupFrequency)
    fun updateBackupGoogleAccount(account: String)
    fun updateBackupUseCellular(enabled: Boolean)
    fun updateBackupDestinationOrdinals(ordinals: Set<Int>)
}
