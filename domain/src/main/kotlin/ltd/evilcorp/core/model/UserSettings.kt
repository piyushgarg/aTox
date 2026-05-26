package ltd.evilcorp.core.model

import ltd.evilcorp.core.tox.save.ProxyType

const val DEFAULT_THEME_MODE = -1
const val DEFAULT_ACCENT_COLOR_SEED = 0xFF3F51B5.toInt()

enum class TimeFormatPreference {
    System,
    Hours24,
    Hours12,
}

enum class DateFormatPreference {
    System,
    DMY,
    DMYDots,
    MDY,
    YMD,
}

enum class AppSound {
    SoftPop,
    SoftTick,
    SoftBeep,
    Glass,
    Pulse,
    Ripple,
}

enum class BackupDestination {
    Local,
    GoogleDrive,
    Nextcloud,
    WebDav,
}

enum class BackupFrequency {
    Off,
    Daily,
    Weekly,
    Monthly,
}

data class UserSettings(
    val themeMode: Int = DEFAULT_THEME_MODE,
    val dynamicColorEnabled: Boolean = true,
    val accentColorSeed: Int = DEFAULT_ACCENT_COLOR_SEED,
    val localeTag: String = "",
    val dateFormatPreference: DateFormatPreference = DateFormatPreference.System,
    val timeFormatPreference: TimeFormatPreference = TimeFormatPreference.System,
    val udpEnabled: Boolean = false,
    val runAtStartup: Boolean = false,
    val autoAwayEnabled: Boolean = false,
    val autoAwaySeconds: Long = 180L,
    val proxyType: ProxyType = ProxyType.None,
    val proxyAddress: String = "",
    val proxyPort: Int = 0,
    val ftAutoAccept: FtAutoAccept = FtAutoAccept.None,
    val bootstrapNodeSource: BootstrapNodeSource = BootstrapNodeSource.BuiltIn,
    val disableScreenshots: Boolean = false,
    val confirmQuitting: Boolean = true,
    val confirmCalling: Boolean = true,
    val sentMessageSoundVolume: Int = 24,
    val sentMessageSoundUri: String = "",
    val callSound: AppSound = AppSound.Pulse,
    val callSoundVolume: Int = 72,
    val callRingtoneUri: String = "",
    val notificationSoundVolume: Int = 52,
    val notificationSoundUri: String = "",
    val activeChatSoundVolume: Int = 28,
    val activeChatSoundUri: String = "",
    val hapticEnabled: Boolean = true,
    val autoSaveToDownloads: Boolean = true,
    val autoSaveDirectoryUri: String = "",
    val backupEncryptionEnabled: Boolean = false,
    val backupEndToEndEncryptionEnabled: Boolean = false,
    val automaticBackupEnabled: Boolean = false,
    val backupFrequency: BackupFrequency = BackupFrequency.Off,
    val backupGoogleAccount: String = "",
    val backupUseCellular: Boolean = false,
    val backupDestinationOrdinals: Set<Int> = setOf(BackupDestination.Local.ordinal),
    val enableReplies: Boolean = true,
)
