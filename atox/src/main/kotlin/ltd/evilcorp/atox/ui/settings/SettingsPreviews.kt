// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.ui.settings.common.SettingsRootContent
import ltd.evilcorp.atox.ui.settings.screens.BackupSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.LanguageSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.ThemeSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.NotificationSettingsScreen
import ltd.evilcorp.atox.ui.theme.AToxTheme
import ltd.evilcorp.domain.features.settings.model.BackupDestination
import ltd.evilcorp.domain.features.settings.model.BackupFrequency

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
        NotificationSettingsScreen(
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
            automaticBackupEnabled = true,
            backupFrequency = BackupFrequency.Weekly,
            backupUseCellular = false,
            backupDestinations = setOf(BackupDestination.Local),
            backupGoogleAccount = "user@example.com",
            lastLocalBackupTimeMs = System.currentTimeMillis() - 86400000,
            lastLocalBackupSizeKb = 1024 * 5,
            lastGoogleBackupTimeMs = System.currentTimeMillis() - 86400000 * 2,
            lastGoogleBackupSizeKb = 1024 * 5,
            selectedBackupIds = setOf("tox", "chat"),
            mandatoryBackupId = "profile",
            onAutomaticBackupEnabledChanged = {},
            onBackupFrequencyChanged = {},
            onBackupUseCellularChanged = {},
            onBackupDestinationsChanged = {},
            onGoogleAccountClick = {},
            onSelectedBackupIdsChanged = {},
            onCreateBackupClick = {},
            onCreateGoogleBackupClick = {},
            onRestoreBackupClick = {},
            performHaptic = {}
        )
    }
}

@Preview(name = "Language Settings Screen Preview", showSystemUi = true)
@Composable
fun LanguageSelectionScreenPreview() {
    AToxTheme {
        LanguageSettingsScreen(
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
        ThemeSettingsScreen(
            paddingValues = PaddingValues(16.dp),
            appThemeMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            onThemeSelect = {}
        )
    }
}
