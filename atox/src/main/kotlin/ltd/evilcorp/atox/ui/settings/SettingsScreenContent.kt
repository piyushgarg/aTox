// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.atox.ui.settings.backup.BackupSettingsViewModel
import ltd.evilcorp.atox.ui.settings.common.SettingsDestination
import ltd.evilcorp.atox.ui.settings.common.SettingsRootContent
import ltd.evilcorp.atox.ui.settings.common.SearchableSetting
import ltd.evilcorp.atox.ui.settings.common.SettingsSearchPopup
import ltd.evilcorp.atox.ui.settings.screens.BackupSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.LanguageSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.NetworkSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.NotificationSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.SettingsAppearanceScreen
import ltd.evilcorp.atox.ui.settings.screens.SettingsChatScreen
import ltd.evilcorp.atox.ui.settings.screens.SoundPickerTarget
import ltd.evilcorp.atox.ui.settings.screens.ThemeSettingsScreen

@Suppress(
    "FunctionNaming",
    "LongParameterList",
    "ComplexMethod",
    "LongMethod",
    "UnstableCollections",
    "ViewModelForwarding"
)
@Composable
internal fun SettingsScreenContent(
    state: SettingsScreenState,
    paddingValues: PaddingValues,
    storedSettings: UserSettings,
    appearance: AppAppearance,
    settings: Settings,
    context: Context,
    languages: List<Pair<String, String>>,
    currentLanguageCode: String,
    timeFormatPreference: ltd.evilcorp.domain.features.settings.model.TimeFormatPreference,
    dateFormatPreference: ltd.evilcorp.domain.features.settings.model.DateFormatPreference,
    performHaptic: () -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onThemeChanged: (Int) -> Unit,
    onLocaleTagChanged: (String) -> Unit,
    autoSaveDirectoryLabel: String,
    launchers: SettingsLaunchers,
    viewModel: SettingsViewModel,
    bootstrapNodeSource: ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource,
    proxyType: ltd.evilcorp.domain.features.settings.model.ProxyType,
    proxyAddress: String,
    onDisableScreenshotsChanged: (Boolean) -> Unit,
    focusManager: FocusManager,
    backupViewModel: BackupSettingsViewModel,
    backupExporting: Boolean,
    backupImporting: Boolean,
    mandatoryBackupId: String,
    searchItems: List<SearchableSetting>
) {
    val ftAutoAccept = storedSettings.ftAutoAccept
    val currentLanguageLabel = languages.find {
        it.first == currentLanguageCode
    }?.second ?: "English"

    when (state.destination) {
        SettingsDestination.Root -> SettingsRootContent(
            paddingValues = paddingValues,
            currentLanguageLabel = currentLanguageLabel,
            themeLabel = when (appearance.themeMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> stringResource(R.string.pref_theme_dark)
                AppCompatDelegate.MODE_NIGHT_NO -> stringResource(R.string.pref_theme_light)
                else -> stringResource(R.string.pref_theme_follow_system)
            },
            onAppearanceClick = { state.destination = SettingsDestination.Appearance },
            onChatClick = { state.destination = SettingsDestination.Chat },
            onSoundsClick = { state.destination = SettingsDestination.Sounds },
            onConnectionClick = { state.destination = SettingsDestination.Connection },
            onBackupClick = { state.destination = SettingsDestination.Backup }
        )
        SettingsDestination.Appearance -> SettingsAppearanceScreen(
            paddingValues = paddingValues,
            currentLanguageCode = currentLanguageCode,
            languages = languages,
            appThemeMode = appearance.themeMode,
            timeFormatPreference = timeFormatPreference,
            dateFormatPreference = dateFormatPreference,
            dynamicColor = appearance.dynamicColorEnabled,
            currentAccentSeed = appearance.accentColorSeed,
            hapticEnabled = storedSettings.hapticEnabled,
            performHaptic = performHaptic,
            onLanguageClick = { state.destination = SettingsDestination.Language },
            onThemeClick = { state.destination = SettingsDestination.Theme },
            onDateFormatClick = { state.showDateFormatDialog = true },
            onTimeFormatClick = { state.showTimeFormatDialog = true },
            onDynamicColorChanged = onDynamicColorChanged,
            onAccentColorClick = { state.showAccentColorDialog = true },
            onHapticEnabledChanged = { settings.hapticEnabled = it }
        )
        SettingsDestination.Chat -> SettingsChatScreen(
            paddingValues = paddingValues,
            ftAutoAccept = ftAutoAccept,
            autoSaveToDownloads = storedSettings.autoSaveToDownloads,
            autoSaveDirectoryLabel = autoSaveDirectoryLabel,
            cacheSizeText = state.cacheSizeText,
            enableReplies = storedSettings.enableReplies,
            performHaptic = performHaptic,
            onFtAutoAcceptClick = { viewModel.setShowFtAcceptDialog(true) },
            onAutoSaveToDownloadsChanged = { settings.autoSaveToDownloads = it },
            onAutoSaveDirectoryClick = { launchers.autoSaveDirectoryLauncher.launch(null) },
            onClearCacheClick = {
                viewModel.clearCache()
                state.cacheSizeText = formatSize(context, 0)
            },
            onEnableRepliesChanged = { settings.enableReplies = it }
        )
        SettingsDestination.Connection -> NetworkSettingsScreen(
            paddingValues = paddingValues,
            udpEnabled = storedSettings.udpEnabled,
            runAtStartup = storedSettings.runAtStartup,
            bootstrapNodeSource = bootstrapNodeSource,
            disableScreenshots = storedSettings.disableScreenshots,
            confirmQuitting = storedSettings.confirmQuitting,
            confirmCalling = storedSettings.confirmCalling,
            proxyType = proxyType,
            proxyAddress = proxyAddress,
            proxyPortInput = state.proxyPortInput,
            focusManager = focusManager,
            performHaptic = performHaptic,
            onUdpEnabledChanged = viewModel::setUdpEnabled,
            onRunAtStartupChanged = viewModel::setRunAtStartup,
            onBootstrapNodesClick = { viewModel.setShowBootstrapDialog(true) },
            onDisableScreenshotsChanged = onDisableScreenshotsChanged,
            onConfirmQuittingChanged = { settings.confirmQuitting = it },
            onConfirmCallingChanged = { settings.confirmCalling = it },
            onProxyTypeClick = { viewModel.setShowProxyDialog(true) },
            onProxyAddressChanged = { settings.proxyAddress = it },
            onProxyPortInputChanged = {
                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                    state.proxyPortInput = it
                    viewModel.setProxyPortString(it)
                }
            }
        )
        SettingsDestination.Language -> LanguageSettingsScreen(
            paddingValues = paddingValues,
            currentLanguageCode = currentLanguageCode,
            onLanguageSelect = { localeTag ->
                state.destination = SettingsDestination.Appearance
                onLocaleTagChanged(localeTag)
            }
        )
        SettingsDestination.Theme -> ThemeSettingsScreen(
            paddingValues = paddingValues,
            appThemeMode = appearance.themeMode,
            onThemeSelect = { themeMode ->
                state.destination = SettingsDestination.Appearance
                onThemeChanged(themeMode)
            }
        )
        SettingsDestination.Search -> SettingsSearchPopup(
            searchQuery = state.searchQuery,
            onSearchQueryChange = { state.searchQuery = it },
            searchItems = searchItems,
            onDismissRequest = {
                state.searchQuery = ""
                state.destination = SettingsDestination.Root
            },
            performHaptic = performHaptic,
            onItemClick = { item ->
                if (item.onTrigger != null) {
                    item.onTrigger.invoke()
                } else {
                    state.destination = item.destination
                }
            }
        )
        SettingsDestination.Sounds -> NotificationSettingsScreen(
            paddingValues = paddingValues,
            sentMessageSoundVolume = storedSettings.sentMessageSoundVolume,
            callSoundVolume = storedSettings.callSoundVolume,
            notificationSoundVolume = storedSettings.notificationSoundVolume,
            activeChatSoundVolume = storedSettings.activeChatSoundVolume,
            sentMessageSoundUri = storedSettings.sentMessageSoundUri,
            callRingtoneUri = storedSettings.callRingtoneUri,
            notificationSoundUri = storedSettings.notificationSoundUri,
            activeChatSoundUri = storedSettings.activeChatSoundUri,
            onVolumeChanged = { target, volume ->
                when (target) {
                    SoundPickerTarget.Sent -> settings.sentMessageSoundVolume = volume
                    SoundPickerTarget.Call -> settings.callSoundVolume = volume
                    SoundPickerTarget.Notification -> settings.notificationSoundVolume = volume
                    SoundPickerTarget.ActiveChat -> settings.activeChatSoundVolume = volume
                }
            },
            onSoundPickerClick = { target, currentUri, type ->
                state.soundPickerTarget = target
                launchRingtonePicker(
                    launchers.ringtonePickerLauncher,
                    when (target) {
                        SoundPickerTarget.Sent -> context.getString(R.string.settings_sent_sound_title)
                        SoundPickerTarget.Call -> context.getString(R.string.settings_call_sound_title)
                        SoundPickerTarget.Notification -> context.getString(R.string.settings_notification_sound_title)
                        SoundPickerTarget.ActiveChat -> context.getString(R.string.settings_active_chat_sound_title)
                    },
                    type,
                    currentUri
                )
            },
            performHaptic = performHaptic
        )
        SettingsDestination.Backup -> BackupSettingsScreen(
            paddingValues = paddingValues,
            backupProviders = backupViewModel.backupProviders,
            backupExporting = backupExporting,
            backupImporting = backupImporting,
            backupPasswordEnabled = state.backupPasswordEnabled,
            backupPassword = state.backupPassword,
            backupPasswordVisible = false,
            automaticBackupEnabled = storedSettings.automaticBackupEnabled,
            backupFrequency = storedSettings.backupFrequency,
            backupUseCellular = storedSettings.backupUseCellular,
            backupDestinations = settings.backupDestinations,
            backupEndToEndEncryptionEnabled = storedSettings.backupEndToEndEncryptionEnabled,
            backupGoogleAccount = storedSettings.backupGoogleAccount,
            selectedBackupIds = state.selectedBackupIds,
            mandatoryBackupId = mandatoryBackupId,
            onBackupPasswordEnabledChanged = { state.backupPasswordEnabled = it },
            onBackupPasswordChanged = { state.backupPassword = it },
            onBackupPasswordVisibleChanged = { /* unused */ },
            onAutomaticBackupEnabledChanged = { settings.automaticBackupEnabled = it },
            onBackupFrequencyChanged = viewModel::setBackupFrequency,
            onBackupUseCellularChanged = { settings.backupUseCellular = it },
            onBackupDestinationsChanged = viewModel::setBackupDestinations,
            onBackupEndToEndEncryptionEnabledChanged = {
                settings.backupEndToEndEncryptionEnabled = it
            },
            onGoogleAccountClick = { state.showGoogleAccountDialog = true },
            onSelectedBackupIdsChanged = { state.selectedBackupIds = it },
            onCreateBackupClick = { launchers.backupLauncher.launch("atox-backup.zip") },
            onRestoreBackupClick = {
                launchers.restoreBackupLauncher.launch(arrayOf("application/zip"))
            },
            performHaptic = performHaptic
        )
    }
}
