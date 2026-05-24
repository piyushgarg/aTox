package ltd.evilcorp.atox.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.ui.settings.components.*
import ltd.evilcorp.atox.ui.theme.AToxTheme
import ltd.evilcorp.core.model.BackupDestination
import ltd.evilcorp.core.model.BackupFrequency
import ltd.evilcorp.core.model.BootstrapNodeSource
import ltd.evilcorp.core.model.DateFormatPreference
import ltd.evilcorp.core.model.FtAutoAccept
import ltd.evilcorp.core.model.TimeFormatPreference
import ltd.evilcorp.core.tox.save.ProxyType

@Preview(name = "Main Settings List Preview", showSystemUi = true)
@Composable
fun SettingsRootContentPreview() {
    AToxTheme {
        SettingsRootContent(
            paddingValues = PaddingValues(16.dp),
            currentLanguageLabel = "English",
            themeLabel = "System default",
            onAppearanceClick = {},
            onChatClick = {},
            onSoundsClick = {},
            onConnectionClick = {},
            onBackupClick = {}
        )
    }
}

@Preview(name = "Sound Settings Screen Preview", showSystemUi = true)
@Composable
fun SoundSettingsScreenPreview() {
    AToxTheme {
        SoundSettingsScreen(
            paddingValues = PaddingValues(16.dp),
            sentMessageSoundVolume = 80,
            callSoundVolume = 90,
            notificationSoundVolume = 100,
            activeChatSoundVolume = 50,
            sentMessageSoundUri = "",
            callRingtoneUri = "",
            notificationSoundUri = "",
            activeChatSoundUri = "",
            onVolumeChanged = { _, _ -> },
            onSoundPickerClick = { _, _, _ -> },
            performHaptic = {}
        )
    }
}

@Preview(name = "Backup Settings Screen Preview", showSystemUi = true)
@Composable
fun BackupSettingsScreenPreview() {
    AToxTheme {
        BackupSettingsScreen(
            paddingValues = PaddingValues(16.dp),
            backupProviders = emptyList(),
            backupExporting = false,
            backupImporting = false,
            backupPasswordEnabled = true,
            backupPassword = "secure_password",
            backupPasswordVisible = false,
            automaticBackupEnabled = true,
            backupFrequency = BackupFrequency.Weekly,
            backupUseCellular = false,
            backupDestinations = setOf(BackupDestination.Local),
            backupEndToEndEncryptionEnabled = true,
            backupGoogleAccount = "user@gmail.com",
            selectedBackupIds = emptySet(),
            mandatoryBackupId = "profile",
            onBackupPasswordEnabledChanged = {},
            onBackupPasswordChanged = {},
            onBackupPasswordVisibleChanged = {},
            onAutomaticBackupEnabledChanged = {},
            onBackupFrequencyChanged = {},
            onBackupUseCellularChanged = {},
            onBackupDestinationsChanged = {},
            onBackupEndToEndEncryptionEnabledChanged = {},
            onGoogleAccountClick = {},
            onSelectedBackupIdsChanged = {},
            onCreateBackupClick = {},
            onRestoreBackupClick = {},
            performHaptic = {}
        )
    }
}

@Preview(name = "Language Settings Screen Preview", showSystemUi = true)
@Composable
fun LanguageSelectionScreenPreview() {
    AToxTheme {
        LanguageSelectionScreen(
            paddingValues = PaddingValues(16.dp),
            currentLanguageCode = "en",
            onLanguageSelect = {}
        )
    }
}

@Preview(name = "Theme Settings Screen Preview", showSystemUi = true)
@Composable
fun ThemeSelectionScreenPreview() {
    AToxTheme {
        ThemeSelectionScreen(
            paddingValues = PaddingValues(16.dp),
            appThemeMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            onThemeSelect = {}
        )
    }
}
