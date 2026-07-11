// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

@file:Suppress("DEPRECATION")
package ltd.evilcorp.atox.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
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
import ltd.evilcorp.atox.ui.settings.screens.GeneralSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.PrivacySettingsScreen

@Suppress(
    "FunctionNaming",
    "LongParameterList",
    "ComplexMethod",
    "LongMethod",
    "UnstableCollections",
    "ViewModelForwarding",
    "UnusedParameter"
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
            onGeneralClick = { state.destination = SettingsDestination.General },
            onAppearanceClick = { state.destination = SettingsDestination.Appearance },
            onChatClick = { state.destination = SettingsDestination.Chat },
            onSoundsClick = { state.destination = SettingsDestination.Sounds },
            onPrivacyClick = { state.destination = SettingsDestination.Privacy },
            onConnectionClick = { state.destination = SettingsDestination.Connection },
            onBackupClick = { state.destination = SettingsDestination.Backup }
        )
        SettingsDestination.Appearance -> {
            SettingsAppearanceScreen(
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
        }
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
        SettingsDestination.Connection -> {
            NetworkSettingsScreen(
                paddingValues = paddingValues,
                udpEnabled = storedSettings.udpEnabled,
                bootstrapNodeSource = bootstrapNodeSource,
                proxyType = proxyType,
                proxyAddress = proxyAddress,
                proxyPortInput = state.proxyPortInput,
                focusManager = focusManager,
                performHaptic = performHaptic,
                onUdpEnabledChanged = viewModel::setUdpEnabled,
                onBootstrapNodesClick = { viewModel.setShowBootstrapDialog(true) },
                onProxyTypeClick = { viewModel.setShowProxyDialog(true) },
                onProxyAddressChanged = viewModel::setProxyAddress,
                onProxyPortInputChanged = {
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                        state.proxyPortInput = it
                        viewModel.setProxyPortString(it)
                    }
                }
            )
        }
        SettingsDestination.General -> {
            val showProfilePicker = androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(ltd.evilcorp.core.profile.ProfileManager.getShowProfilePicker(context))
            }
            GeneralSettingsScreen(
                paddingValues = paddingValues,
                showProfilePicker = showProfilePicker.value,
                runAtStartup = storedSettings.runAtStartup,
                confirmQuitting = storedSettings.confirmQuitting,
                performHaptic = performHaptic,
                onShowProfilePickerChanged = {
                    ltd.evilcorp.core.profile.ProfileManager.setShowProfilePicker(context, it)
                    showProfilePicker.value = it
                },
                onRunAtStartupChanged = viewModel::setRunAtStartup,
                onConfirmQuittingChanged = { settings.confirmQuitting = it }
            )
        }
        SettingsDestination.Privacy -> {
            val biometricManager = androidx.compose.runtime.remember(context) { androidx.biometric.BiometricManager.from(context) }
            val isBiometricHardwareAvailable = androidx.compose.runtime.remember(biometricManager) {
                biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) !=
                        androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
            }
            var biometricEnabled by androidx.compose.runtime.remember(state.showChangePasswordDialog) {
                androidx.compose.runtime.mutableStateOf(ltd.evilcorp.atox.infrastructure.security.BiometricStorage.isBiometricEnabled(context))
            }
            PrivacySettingsScreen(
                paddingValues = paddingValues,
                disableScreenshots = storedSettings.disableScreenshots,
                confirmCalling = storedSettings.confirmCalling,
                isBiometricHardwareAvailable = isBiometricHardwareAvailable,
                biometricEnabled = biometricEnabled,
                hasPassword = viewModel.hasPassword(),
                performHaptic = performHaptic,
                onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                onConfirmCallingChanged = { settings.confirmCalling = it },
                onChangePasswordClick = { state.showChangePasswordDialog = true },
                onBiometricEnabledChanged = { enabled ->
                    if (enabled) {
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        if (!viewModel.hasPassword()) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.biometric_setup_failed),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else if (activity != null) {
                            val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
                            val biometricPrompt = androidx.biometric.BiometricPrompt(
                                activity,
                                executor,
                                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        super.onAuthenticationError(errorCode, errString)
                                        android.widget.Toast.makeText(context, errString, android.widget.Toast.LENGTH_LONG).show()
                                    }

                                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                        super.onAuthenticationSucceeded(result)
                                        val unlockedCipher = result.cryptoObject?.cipher
                                        if (unlockedCipher != null) {
                                            val success = viewModel.enableBiometric(context, unlockedCipher)
                                            if (success) {
                                                biometricEnabled = true
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.biometric_setup_failed),
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            )
                            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                .setTitle(context.getString(R.string.pref_biometric_login))
                                .setSubtitle(context.getString(R.string.pref_biometric_login_description))
                                .setNegativeButtonText(context.getString(android.R.string.cancel))
                                .build()

                            try {
                                val cipher = ltd.evilcorp.atox.infrastructure.security.BiometricCipherHelper.getInitializedCipherForEncryption()
                                biometricPrompt.authenticate(promptInfo, androidx.biometric.BiometricPrompt.CryptoObject(cipher))
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsScreenContent", "Failed to init cipher: $e", e)
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.biometric_setup_failed),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.biometric_setup_failed),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        viewModel.disableBiometric(context)
                        biometricEnabled = false
                    }
                }
            )
        }
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
            backupExporting = backupExporting,
            backupImporting = backupImporting,
            backupFrequency = storedSettings.backupFrequency,
            backupUseCellular = storedSettings.backupUseCellular,
            backupGoogleAccount = storedSettings.backupGoogleAccount,
            lastLocalBackupTimeMs = storedSettings.lastLocalBackupTimeMs,
            lastLocalBackupSizeKb = storedSettings.lastLocalBackupSizeKb,
            lastGoogleBackupTimeMs = storedSettings.lastGoogleBackupTimeMs,
            lastGoogleBackupSizeKb = storedSettings.lastGoogleBackupSizeKb,
            onBackupFrequencyChanged = { freq ->
                viewModel.setBackupFrequency(freq)
                settings.automaticBackupEnabled = (freq != BackupFrequency.Off)
            },
            onBackupUseCellularChanged = { settings.backupUseCellular = it },
            onGoogleAccountClick = {
                state.googleSignInPurpose = GoogleSignInPurpose.Connect
                val googleSignInOptions = ltd.evilcorp.atox.infrastructure.backup.google.GoogleDriveBackupHelper.getSignInOptions()
                val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, googleSignInOptions)
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                if (account != null) {
                    client.signOut().addOnCompleteListener {
                        launchers.googleSignInLauncher.launch(client.signInIntent)
                    }
                } else {
                    launchers.googleSignInLauncher.launch(client.signInIntent)
                }
            },
            onCreateBackupClick = backupViewModel::createBackup,
            onRestoreBackupClick = {
                launchers.restoreBackupLauncher.launch(arrayOf("*/*"))
            },
            onRestoreGoogleBackupClick = {
                state.googleSignInPurpose = GoogleSignInPurpose.Restore
                val googleSignInOptions = ltd.evilcorp.atox.infrastructure.backup.google.GoogleDriveBackupHelper.getSignInOptions()
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                val hasScopes = account != null && com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(
                    account,
                    com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE),
                    com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_APPDATA)
                )
                if (hasScopes) {
                    backupViewModel.listGoogleDriveBackups()
                    state.showGoogleDriveRestoreDialog = true
                } else {
                    val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, googleSignInOptions)
                    if (account != null) {
                        client.signOut().addOnCompleteListener {
                            launchers.googleSignInLauncher.launch(client.signInIntent)
                        }
                    } else {
                        launchers.googleSignInLauncher.launch(client.signInIntent)
                    }
                }
            },
            localBackupDirectoryUri = storedSettings.localBackupDirectoryUri,
            onLocalBackupDirectoryClick = {
                launchers.localBackupDirectoryLauncher.launch(null)
            },
            performHaptic = performHaptic
        )
    }
}
