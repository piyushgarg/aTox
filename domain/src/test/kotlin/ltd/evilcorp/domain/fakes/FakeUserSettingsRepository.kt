package ltd.evilcorp.domain.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.domain.features.settings.model.*
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository

class FakeUserSettingsRepository : IUserSettingsRepository {
    private val _settings = MutableStateFlow(UserSettings())
    override val settings: StateFlow<UserSettings> = _settings

    override suspend fun updateThemeMode(themeMode: Int) {
        _settings.value = _settings.value.copy(themeMode = themeMode)
    }

    override suspend fun updateDynamicColorEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(dynamicColorEnabled = enabled)
    }

    override suspend fun updateAccentColorSeed(accentColorSeed: Int) {
        _settings.value = _settings.value.copy(accentColorSeed = accentColorSeed)
    }

    override suspend fun updateLocaleTag(localeTag: String) {
        _settings.value = _settings.value.copy(localeTag = localeTag)
    }

    override suspend fun updateDateFormatPreference(preference: DateFormatPreference) {
        _settings.value = _settings.value.copy(dateFormatPreference = preference)
    }

    override suspend fun updateTimeFormatPreference(preference: TimeFormatPreference) {
        _settings.value = _settings.value.copy(timeFormatPreference = preference)
    }

    override suspend fun updateUdpEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(udpEnabled = enabled)
    }

    override suspend fun updateRunAtStartup(enabled: Boolean) {
        _settings.value = _settings.value.copy(runAtStartup = enabled)
    }

    override suspend fun updateAutoAwayEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoAwayEnabled = enabled)
    }

    override suspend fun updateAutoAwaySeconds(seconds: Long) {
        _settings.value = _settings.value.copy(autoAwaySeconds = seconds)
    }

    override suspend fun updateProxyType(type: ProxyType) {
        _settings.value = _settings.value.copy(proxyType = type)
    }

    override suspend fun updateProxyAddress(address: String) {
        _settings.value = _settings.value.copy(proxyAddress = address)
    }

    override suspend fun updateProxyPort(port: Int) {
        _settings.value = _settings.value.copy(proxyPort = port)
    }

    override suspend fun updateFtAutoAccept(value: FtAutoAccept) {
        _settings.value = _settings.value.copy(ftAutoAccept = value)
    }

    override suspend fun updateBootstrapNodeSource(value: BootstrapNodeSource) {
        _settings.value = _settings.value.copy(bootstrapNodeSource = value)
    }

    override suspend fun updateDisableScreenshots(disable: Boolean) {
        _settings.value = _settings.value.copy(disableScreenshots = disable)
    }

    override suspend fun updateConfirmQuitting(confirm: Boolean) {
        _settings.value = _settings.value.copy(confirmQuitting = confirm)
    }

    override suspend fun updateConfirmCalling(confirm: Boolean) {
        _settings.value = _settings.value.copy(confirmCalling = confirm)
    }

    override suspend fun updateEnableReplies(enabled: Boolean) {
        _settings.value = _settings.value.copy(enableReplies = enabled)
    }

    override suspend fun updateSentMessageSoundVolume(volume: Int) {
        _settings.value = _settings.value.copy(sentMessageSoundVolume = volume)
    }

    override suspend fun updateSentMessageSoundUri(uri: String) {
        _settings.value = _settings.value.copy(sentMessageSoundUri = uri)
    }

    override suspend fun updateCallSound(sound: AppSound) {
        _settings.value = _settings.value.copy(callSound = sound)
    }

    override suspend fun updateCallSoundVolume(volume: Int) {
        _settings.value = _settings.value.copy(callSoundVolume = volume)
    }

    override suspend fun updateCallRingtoneUri(uri: String) {
        _settings.value = _settings.value.copy(callRingtoneUri = uri)
    }

    override suspend fun updateNotificationSoundVolume(volume: Int) {
        _settings.value = _settings.value.copy(notificationSoundVolume = volume)
    }

    override suspend fun updateNotificationSoundUri(uri: String) {
        _settings.value = _settings.value.copy(notificationSoundUri = uri)
    }

    override suspend fun updateActiveChatSoundVolume(volume: Int) {
        _settings.value = _settings.value.copy(activeChatSoundVolume = volume)
    }

    override suspend fun updateActiveChatSoundUri(uri: String) {
        _settings.value = _settings.value.copy(activeChatSoundUri = uri)
    }

    override suspend fun updateHapticEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(hapticEnabled = enabled)
    }

    override suspend fun updateAutoSaveToDownloads(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoSaveToDownloads = enabled)
    }

    override suspend fun updateAutoSaveDirectoryUri(uri: String) {
        _settings.value = _settings.value.copy(autoSaveDirectoryUri = uri)
    }


    override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(automaticBackupEnabled = enabled)
    }

    override suspend fun updateBackupFrequency(frequency: BackupFrequency) {
        _settings.value = _settings.value.copy(backupFrequency = frequency)
    }

    override suspend fun updateBackupGoogleAccount(account: String) {
        _settings.value = _settings.value.copy(backupGoogleAccount = account)
    }

    override suspend fun updateBackupUseCellular(enabled: Boolean) {
        _settings.value = _settings.value.copy(backupUseCellular = enabled)
    }

    override suspend fun updateBackupDestinationOrdinals(ordinals: Set<Int>) {
        _settings.value = _settings.value.copy(backupDestinationOrdinals = ordinals)
    }

    override suspend fun updateLastLocalBackupTimeMs(timeMs: Long) {
        _settings.value = _settings.value.copy(lastLocalBackupTimeMs = timeMs)
    }

    override suspend fun updateLastLocalBackupSizeKb(sizeKb: Long) {
        _settings.value = _settings.value.copy(lastLocalBackupSizeKb = sizeKb)
    }

    override suspend fun updateLastGoogleBackupTimeMs(timeMs: Long) {
        _settings.value = _settings.value.copy(lastGoogleBackupTimeMs = timeMs)
    }

    override suspend fun updateLastGoogleBackupSizeKb(sizeKb: Long) {
        _settings.value = _settings.value.copy(lastGoogleBackupSizeKb = sizeKb)
    }
}
