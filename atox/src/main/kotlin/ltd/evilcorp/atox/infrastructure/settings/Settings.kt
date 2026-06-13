package ltd.evilcorp.atox.infrastructure.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.settings.model.BackupDestination
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.FtAutoAccept
import ltd.evilcorp.domain.features.settings.model.AppSound
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import ltd.evilcorp.atox.infrastructure.receiver.BootReceiver
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository

class Settings @Inject constructor(
    private val ctx: Context,
    private val repository: IUserSettingsRepository,
    private val scope: CoroutineScope,
) {

    val state: StateFlow<UserSettings> = repository.settings

    init {
        val persisted = repository.settings.value.runAtStartup
        val actual = isBootReceiverEnabled()
        if (persisted != actual) {
            scope.launch {
                repository.updateRunAtStartup(actual)
            }
        }
    }

    var udpEnabled: Boolean
        get() = repository.settings.value.udpEnabled
        set(enabled) {
            scope.launch { repository.updateUdpEnabled(enabled) }
        }

    var runAtStartup: Boolean
        get() = repository.settings.value.runAtStartup
        set(runAtStartup) {
            scope.launch { repository.updateRunAtStartup(runAtStartup) }
            val state = if (runAtStartup) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            ctx.packageManager.setComponentEnabledSetting(
                ComponentName(ctx, BootReceiver::class.java),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }

    var autoAwayEnabled: Boolean
        get() = repository.settings.value.autoAwayEnabled
        set(enabled) {
            scope.launch { repository.updateAutoAwayEnabled(enabled) }
        }

    var autoAwaySeconds: Long
        get() = repository.settings.value.autoAwaySeconds
        set(seconds) {
            scope.launch { repository.updateAutoAwaySeconds(seconds) }
        }

    var proxyType: ProxyType
        get() = repository.settings.value.proxyType
        set(type) {
            scope.launch { repository.updateProxyType(type) }
        }

    var proxyAddress: String
        get() = repository.settings.value.proxyAddress
        set(address) {
            scope.launch { repository.updateProxyAddress(address) }
        }

    var dateFormatPreference: DateFormatPreference
        get() = repository.settings.value.dateFormatPreference
        set(preference) {
            scope.launch { repository.updateDateFormatPreference(preference) }
        }

    var timeFormatPreference: TimeFormatPreference
        get() = repository.settings.value.timeFormatPreference
        set(preference) {
            scope.launch { repository.updateTimeFormatPreference(preference) }
        }

    var proxyPort: Int
        get() = repository.settings.value.proxyPort
        set(port) {
            scope.launch { repository.updateProxyPort(port) }
        }

    var ftAutoAccept: FtAutoAccept
        get() = repository.settings.value.ftAutoAccept
        set(autoAccept) {
            scope.launch { repository.updateFtAutoAccept(autoAccept) }
        }

    var bootstrapNodeSource: BootstrapNodeSource
        get() = repository.settings.value.bootstrapNodeSource
        set(source) {
            scope.launch { repository.updateBootstrapNodeSource(source) }
        }

    var disableScreenshots: Boolean
        get() = repository.settings.value.disableScreenshots
        set(disable) {
            scope.launch { repository.updateDisableScreenshots(disable) }
        }

    var confirmQuitting: Boolean
        get() = repository.settings.value.confirmQuitting
        set(confirm) {
            scope.launch { repository.updateConfirmQuitting(confirm) }
        }

    var confirmCalling: Boolean
        get() = repository.settings.value.confirmCalling
        set(confirm) {
            scope.launch { repository.updateConfirmCalling(confirm) }
        }

    var enableReplies: Boolean
        get() = repository.settings.value.enableReplies
        set(enabled) {
            scope.launch { repository.updateEnableReplies(enabled) }
        }

    var sentMessageSoundVolume: Int
        get() = repository.settings.value.sentMessageSoundVolume
        set(volume) {
            scope.launch { repository.updateSentMessageSoundVolume(volume) }
        }

    var sentMessageSoundUri: String
        get() = repository.settings.value.sentMessageSoundUri
        set(uri) {
            scope.launch { repository.updateSentMessageSoundUri(uri) }
        }

    var callSound: AppSound
        get() = repository.settings.value.callSound
        set(sound) {
            scope.launch { repository.updateCallSound(sound) }
        }

    var callSoundVolume: Int
        get() = repository.settings.value.callSoundVolume
        set(volume) {
            scope.launch { repository.updateCallSoundVolume(volume) }
        }

    var callRingtoneUri: String
        get() = repository.settings.value.callRingtoneUri
        set(uri) {
            scope.launch { repository.updateCallRingtoneUri(uri) }
        }

    var notificationSoundVolume: Int
        get() = repository.settings.value.notificationSoundVolume
        set(volume) {
            scope.launch { repository.updateNotificationSoundVolume(volume) }
        }

    var notificationSoundUri: String
        get() = repository.settings.value.notificationSoundUri
        set(uri) {
            scope.launch { repository.updateNotificationSoundUri(uri) }
        }

    var activeChatSoundVolume: Int
        get() = repository.settings.value.activeChatSoundVolume
        set(volume) {
            scope.launch { repository.updateActiveChatSoundVolume(volume) }
        }

    var activeChatSoundUri: String
        get() = repository.settings.value.activeChatSoundUri
        set(uri) {
            scope.launch { repository.updateActiveChatSoundUri(uri) }
        }

    var hapticEnabled: Boolean
        get() = repository.settings.value.hapticEnabled
        set(enabled) {
            scope.launch { repository.updateHapticEnabled(enabled) }
        }

    var autoSaveToDownloads: Boolean
        get() = repository.settings.value.autoSaveToDownloads
        set(enabled) {
            scope.launch { repository.updateAutoSaveToDownloads(enabled) }
        }

    var autoSaveDirectoryUri: String
        get() = repository.settings.value.autoSaveDirectoryUri
        set(uri) {
            scope.launch { repository.updateAutoSaveDirectoryUri(uri) }
        }


    var automaticBackupEnabled: Boolean
        get() = repository.settings.value.automaticBackupEnabled
        set(enabled) {
            scope.launch { repository.updateAutomaticBackupEnabled(enabled) }
        }

    var backupFrequency: BackupFrequency
        get() = repository.settings.value.backupFrequency
        set(frequency) {
            scope.launch { repository.updateBackupFrequency(frequency) }
        }

    var backupGoogleAccount: String
        get() = repository.settings.value.backupGoogleAccount
        set(account) {
            scope.launch { repository.updateBackupGoogleAccount(account) }
        }

    var backupUseCellular: Boolean
        get() = repository.settings.value.backupUseCellular
        set(enabled) {
            scope.launch { repository.updateBackupUseCellular(enabled) }
        }

    var lastLocalBackupTimeMs: Long
        get() = repository.settings.value.lastLocalBackupTimeMs
        set(timeMs) {
            scope.launch { repository.updateLastLocalBackupTimeMs(timeMs) }
        }

    var lastLocalBackupSizeKb: Long
        get() = repository.settings.value.lastLocalBackupSizeKb
        set(sizeKb) {
            scope.launch { repository.updateLastLocalBackupSizeKb(sizeKb) }
        }

    var lastGoogleBackupTimeMs: Long
        get() = repository.settings.value.lastGoogleBackupTimeMs
        set(timeMs) {
            scope.launch { repository.updateLastGoogleBackupTimeMs(timeMs) }
        }

    var lastGoogleBackupSizeKb: Long
        get() = repository.settings.value.lastGoogleBackupSizeKb
        set(sizeKb) {
            scope.launch { repository.updateLastGoogleBackupSizeKb(sizeKb) }
        }

    var backupDestinations: Set<BackupDestination>
        get() = repository.settings.value.backupDestinationOrdinals
            .mapNotNull { ordinal -> BackupDestination.entries.getOrNull(ordinal) }
            .toSet()
            .takeIf { it.isNotEmpty() }
            ?: setOf(BackupDestination.Local)
        set(destinations) {
            scope.launch {
                repository.updateBackupDestinationOrdinals(
                    destinations.takeIf { it.isNotEmpty() }?.map { it.ordinal }?.toSet()
                        ?: setOf(BackupDestination.Local.ordinal),
                )
            }
        }

    private fun isBootReceiverEnabled(): Boolean =
        ctx.packageManager.getComponentEnabledSetting(
            ComponentName(ctx, BootReceiver::class.java),
        ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
}
