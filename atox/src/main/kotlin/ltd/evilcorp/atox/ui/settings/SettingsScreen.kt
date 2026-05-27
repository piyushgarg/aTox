// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.common.MorphingNavigationIcon
import ltd.evilcorp.atox.ui.settings.common.SettingsDestination
import ltd.evilcorp.atox.ui.settings.common.SettingsRootContent
import ltd.evilcorp.atox.ui.settings.common.SettingsSearchIndex
import ltd.evilcorp.atox.ui.settings.common.SettingsSearchPopup
import ltd.evilcorp.atox.ui.settings.dialogs.SettingsDialogs
import ltd.evilcorp.atox.ui.settings.backup.BackupSettingsViewModel
import ltd.evilcorp.atox.ui.settings.backup.BackupUiEvent
import ltd.evilcorp.atox.ui.settings.screens.SettingsAppearanceScreen
import ltd.evilcorp.atox.ui.settings.screens.SettingsChatScreen
import ltd.evilcorp.atox.ui.settings.screens.NetworkSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.LanguageSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.ThemeSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.NotificationSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.BackupSettingsScreen
import ltd.evilcorp.atox.ui.settings.screens.SoundPickerTarget

@Suppress("FunctionNaming", "MaxLineLength", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    appearance: AppAppearance,
    onThemeChanged: (Int) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onAccentColorSeedChanged: (Int) -> Unit,
    onLocaleTagChanged: (String) -> Unit,
    onDisableScreenshotsChanged: (Boolean) -> Unit,
    onBack: () -> Unit = {},
    showBackButton: Boolean = true,

    onTitleChanged: (String) -> Unit = {},
    onBackActionChanged: ((() -> Unit)?) -> Unit = {},
    onSearchActionChanged: ((() -> Unit)?) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val storedSettings by settings.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val performHaptic = {
        if (storedSettings.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    var destination by remember { mutableStateOf(SettingsDestination.Root) }
    var searchQuery by remember { mutableStateOf("") }

    val proxyType = storedSettings.proxyType
    val proxyAddress = storedSettings.proxyAddress
    val proxyPort = storedSettings.proxyPort.toString()
    val ftAutoAccept = storedSettings.ftAutoAccept
    val bootstrapNodeSource = storedSettings.bootstrapNodeSource
    val dateFormatPreference = storedSettings.dateFormatPreference
    val timeFormatPreference = storedSettings.timeFormatPreference
    var proxyPortInput by remember { mutableStateOf(proxyPort) }

    val showProxyDialog by viewModel.showProxyDialog.collectAsState()
    val showFtAcceptDialog by viewModel.showFtAcceptDialog.collectAsState()
    val showBootstrapDialog by viewModel.showBootstrapDialog.collectAsState()
    var pendingRestoreUri by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showGoogleAccountDialog by remember { mutableStateOf(false) }
    var googleAccountInput by remember { mutableStateOf("") }

    val accountPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) { googleAccountInput = accountName }
        }
    }

    LaunchedEffect(storedSettings.backupGoogleAccount) { googleAccountInput = storedSettings.backupGoogleAccount }
    var showAccentColorDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }
    var backupPasswordEnabled by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    val mandatoryBackupId = remember(backupViewModel.backupProviders) { backupViewModel.backupProviders.firstOrNull()?.id.orEmpty() }
    var selectedBackupIds by remember(backupViewModel.backupProviders) { mutableStateOf(backupViewModel.backupProviders.map { it.id }.toSet()) }
    var soundPickerTarget by remember { mutableStateOf(SoundPickerTarget.Call) }
    val backupExporting by backupViewModel.backupExporting.collectAsState()
    val backupImporting by backupViewModel.backupImporting.collectAsState()
    
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            backupViewModel.exportBackup(uri.toString(), selectedBackupIds + mandatoryBackupId, backupPassword.takeIf { backupPasswordEnabled })
        }
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri.toString()
            showRestoreConfirmDialog = true
        }
    }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val pickedUri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            when (soundPickerTarget) {
                SoundPickerTarget.Sent -> settings.sentMessageSoundUri = pickedUri?.toString().orEmpty()
                SoundPickerTarget.Call -> settings.callRingtoneUri = pickedUri?.toString().orEmpty()
                SoundPickerTarget.Notification -> settings.notificationSoundUri = pickedUri?.toString().orEmpty()
                SoundPickerTarget.ActiveChat -> settings.activeChatSoundUri = pickedUri?.toString().orEmpty()
            }
        }
    }

    val autoSaveDirectoryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
            settings.autoSaveDirectoryUri = uri.toString()
        }
    }

    val autoSaveDirectoryLabel = remember(storedSettings.autoSaveDirectoryUri) {
        val uriString = storedSettings.autoSaveDirectoryUri
        if (uriString.isBlank()) {
            context.getString(R.string.settings_auto_save_directory_default)
        } else {
            runCatching {
                val uri = Uri.parse(uriString)
                val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                docId.split(":").getOrNull(1) ?: docId
            }.getOrElse { uriString.substringAfterLast("/") }.takeIf { it.isNotBlank() } ?: "Folder"
        }
    }

    var cacheSizeText by remember { mutableStateOf(context.getString(R.string.settings_cache_calculating)) }
    LaunchedEffect(destination) {
        if (destination == SettingsDestination.Chat) {
            val sizeBytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { viewModel.getCacheSize() }
            cacheSizeText = formatSize(context, sizeBytes)
        }
    }

    val currentLanguageCode = remember(appearance.localeTag) { appearance.localeTag.substringBefore('-').substringBefore(',') }
    LaunchedEffect(storedSettings.proxyPort) { proxyPortInput = storedSettings.proxyPort.toString() }

    DisposableEffect(Unit) { onDispose { viewModel.commit() } }

    val systemDefaultLabel = stringResource(R.string.pref_theme_follow_system)
    val languages = remember(systemDefaultLabel) {
        listOf("" to systemDefaultLabel, "en" to "English", "ru" to "Русский", "sv" to "Svenska", "de" to "Deutsch", "es" to "Español", "fr" to "Français", "it" to "Italiano", "uk" to "Українська")
    }

    val searchItems = remember(languages, currentLanguageCode, appearance.themeMode) {
        SettingsSearchIndex.buildSearchIndex(context, languages, currentLanguageCode, appearance.themeMode, viewModel)
    }

    val title = when (destination) {
        SettingsDestination.Root -> stringResource(R.string.settings)
        SettingsDestination.Appearance -> stringResource(R.string.appearance_and_design)
        SettingsDestination.Chat -> stringResource(R.string.settings_ft_group)
        SettingsDestination.Sounds -> stringResource(R.string.settings_sounds_group)
        SettingsDestination.Connection -> stringResource(R.string.settings_network_group)
        SettingsDestination.Backup -> stringResource(R.string.backup_title)
        SettingsDestination.Language -> stringResource(R.string.select_language)
        SettingsDestination.Theme -> stringResource(R.string.settings_app_theme_dialog_title)
        SettingsDestination.Search -> stringResource(R.string.search_settings)
    }

    LaunchedEffect(destination, title, showBackButton) {
        if (!showBackButton) {
            onTitleChanged(title)
            onBackActionChanged(
                if (destination != SettingsDestination.Root) {
                    {
                        destination = when (destination) {
                            SettingsDestination.Language, SettingsDestination.Theme -> SettingsDestination.Appearance
                            SettingsDestination.Search -> SettingsDestination.Root
                            else -> SettingsDestination.Root
                        }
                    }
                } else null
            )
            onSearchActionChanged(
                if (destination == SettingsDestination.Root) {
                    { destination = SettingsDestination.Search }
                } else null
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.uiEvents.flowWithLifecycle(lifecycleOwner.lifecycle).collect { event ->
            when (event) { is SettingsUiEvent.ShowToast -> Toast.makeText(context, context.getString(event.messageResId), Toast.LENGTH_LONG).show() }
        }
    }

    LaunchedEffect(backupViewModel, lifecycleOwner) {
        backupViewModel.uiEvents.flowWithLifecycle(lifecycleOwner.lifecycle).collect { event ->
            when (event) { is BackupUiEvent.ShowToast -> Toast.makeText(context, context.getString(event.messageResId), Toast.LENGTH_LONG).show() }
        }
    }

    @Composable
    fun SettingsScreenContent(paddingValues: PaddingValues) {
        when (destination) {
            SettingsDestination.Root -> SettingsRootContent(
                paddingValues = paddingValues,
                currentLanguageLabel = languages.find { it.first == currentLanguageCode }?.second ?: "English",
                themeLabel = when (appearance.themeMode) {
                    AppCompatDelegate.MODE_NIGHT_YES -> stringResource(R.string.pref_theme_dark)
                    AppCompatDelegate.MODE_NIGHT_NO -> stringResource(R.string.pref_theme_light)
                    else -> stringResource(R.string.pref_theme_follow_system)
                },
                onAppearanceClick = { destination = SettingsDestination.Appearance },
                onChatClick = { destination = SettingsDestination.Chat },
                onSoundsClick = { destination = SettingsDestination.Sounds },
                onConnectionClick = { destination = SettingsDestination.Connection },
                onBackupClick = { destination = SettingsDestination.Backup }
            )
            SettingsDestination.Appearance -> SettingsAppearanceScreen(
                paddingValues = paddingValues, currentLanguageCode = currentLanguageCode, languages = languages, appThemeMode = appearance.themeMode,
                timeFormatPreference = timeFormatPreference, dateFormatPreference = dateFormatPreference, dynamicColor = appearance.dynamicColorEnabled,
                currentAccentSeed = appearance.accentColorSeed, hapticEnabled = storedSettings.hapticEnabled, performHaptic = performHaptic,
                onLanguageClick = { destination = SettingsDestination.Language }, onThemeClick = { destination = SettingsDestination.Theme },
                onDateFormatClick = { showDateFormatDialog = true }, onTimeFormatClick = { showTimeFormatDialog = true },
                onDynamicColorChanged = onDynamicColorChanged, onAccentColorClick = { showAccentColorDialog = true },
                onHapticEnabledChanged = { settings.hapticEnabled = it }
            )
            SettingsDestination.Chat -> SettingsChatScreen(
                paddingValues = paddingValues, ftAutoAccept = ftAutoAccept, autoSaveToDownloads = storedSettings.autoSaveToDownloads,
                autoSaveDirectoryLabel = autoSaveDirectoryLabel, cacheSizeText = cacheSizeText, enableReplies = storedSettings.enableReplies,
                performHaptic = performHaptic, onFtAutoAcceptClick = { viewModel.setShowFtAcceptDialog(true) },
                onAutoSaveToDownloadsChanged = { settings.autoSaveToDownloads = it }, onAutoSaveDirectoryClick = { autoSaveDirectoryLauncher.launch(null) },
                onClearCacheClick = { viewModel.clearCache(); cacheSizeText = formatSize(context, 0) }, onEnableRepliesChanged = { settings.enableReplies = it }
            )
            SettingsDestination.Connection -> NetworkSettingsScreen(
                paddingValues = paddingValues, udpEnabled = storedSettings.udpEnabled, runAtStartup = storedSettings.runAtStartup,
                bootstrapNodeSource = bootstrapNodeSource, disableScreenshots = storedSettings.disableScreenshots, confirmQuitting = storedSettings.confirmQuitting,
                confirmCalling = storedSettings.confirmCalling, proxyType = proxyType, proxyAddress = proxyAddress, proxyPortInput = proxyPortInput,
                focusManager = focusManager, performHaptic = performHaptic, onUdpEnabledChanged = viewModel::setUdpEnabled, onRunAtStartupChanged = viewModel::setRunAtStartup,
                onBootstrapNodesClick = { viewModel.setShowBootstrapDialog(true) }, onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                onConfirmQuittingChanged = { settings.confirmQuitting = it }, onConfirmCallingChanged = { settings.confirmCalling = it },
                onProxyTypeClick = { viewModel.setShowProxyDialog(true) }, onProxyAddressChanged = { settings.proxyAddress = it },
                onProxyPortInputChanged = { if (it.isEmpty() || it.all { char -> char.isDigit() }) { proxyPortInput = it; viewModel.setProxyPortString(it) } }
            )
            SettingsDestination.Language -> LanguageSettingsScreen(
                paddingValues = paddingValues, currentLanguageCode = currentLanguageCode,
                onLanguageSelect = { localeTag -> destination = SettingsDestination.Appearance; onLocaleTagChanged(localeTag) }
            )
            SettingsDestination.Theme -> ThemeSettingsScreen(
                paddingValues = paddingValues, appThemeMode = appearance.themeMode,
                onThemeSelect = { themeMode -> destination = SettingsDestination.Appearance; onThemeChanged(themeMode) }
            )
            SettingsDestination.Search -> SettingsSearchPopup(
                searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it }, searchItems = searchItems,
                onDismissRequest = { searchQuery = ""; destination = SettingsDestination.Root }, performHaptic = performHaptic,
                onItemClick = { item -> if (item.onTrigger != null) { item.onTrigger.invoke() } else { destination = item.destination } }
            )
            SettingsDestination.Sounds -> NotificationSettingsScreen(
                paddingValues = paddingValues, sentMessageSoundVolume = storedSettings.sentMessageSoundVolume, callSoundVolume = storedSettings.callSoundVolume,
                notificationSoundVolume = storedSettings.notificationSoundVolume, activeChatSoundVolume = storedSettings.activeChatSoundVolume,
                sentMessageSoundUri = storedSettings.sentMessageSoundUri, callRingtoneUri = storedSettings.callRingtoneUri,
                notificationSoundUri = storedSettings.notificationSoundUri, activeChatSoundUri = storedSettings.activeChatSoundUri,
                onVolumeChanged = { target, volume ->
                    when (target) {
                        SoundPickerTarget.Sent -> settings.sentMessageSoundVolume = volume
                        SoundPickerTarget.Call -> settings.callSoundVolume = volume
                        SoundPickerTarget.Notification -> settings.notificationSoundVolume = volume
                        SoundPickerTarget.ActiveChat -> settings.activeChatSoundVolume = volume
                    }
                },
                onSoundPickerClick = { target, currentUri, type ->
                    soundPickerTarget = target
                    launchRingtonePicker(ringtonePickerLauncher, when (target) {
                        SoundPickerTarget.Sent -> context.getString(R.string.settings_sent_sound_title)
                        SoundPickerTarget.Call -> context.getString(R.string.settings_call_sound_title)
                        SoundPickerTarget.Notification -> context.getString(R.string.settings_notification_sound_title)
                        SoundPickerTarget.ActiveChat -> context.getString(R.string.settings_active_chat_sound_title)
                    }, type, currentUri)
                },
                performHaptic = performHaptic
            )
            SettingsDestination.Backup -> BackupSettingsScreen(
                paddingValues = paddingValues, backupProviders = backupViewModel.backupProviders, backupExporting = backupExporting,
                backupImporting = backupImporting, backupPasswordEnabled = backupPasswordEnabled, backupPassword = backupPassword,
                backupPasswordVisible = false, automaticBackupEnabled = storedSettings.automaticBackupEnabled, backupFrequency = storedSettings.backupFrequency,
                backupUseCellular = storedSettings.backupUseCellular, backupDestinations = settings.backupDestinations,
                backupEndToEndEncryptionEnabled = storedSettings.backupEndToEndEncryptionEnabled, backupGoogleAccount = storedSettings.backupGoogleAccount,
                selectedBackupIds = selectedBackupIds, mandatoryBackupId = mandatoryBackupId, onBackupPasswordEnabledChanged = { backupPasswordEnabled = it },
                onBackupPasswordChanged = { backupPassword = it }, onBackupPasswordVisibleChanged = { /* unused */ },
                onAutomaticBackupEnabledChanged = { settings.automaticBackupEnabled = it }, onBackupFrequencyChanged = viewModel::setBackupFrequency,
                onBackupUseCellularChanged = { settings.backupUseCellular = it }, onBackupDestinationsChanged = viewModel::setBackupDestinations,
                onBackupEndToEndEncryptionEnabledChanged = { settings.backupEndToEndEncryptionEnabled = it }, onGoogleAccountClick = { showGoogleAccountDialog = true },
                onSelectedBackupIdsChanged = { selectedBackupIds = it }, onCreateBackupClick = { backupLauncher.launch("atox-backup.zip") },
                onRestoreBackupClick = { restoreBackupLauncher.launch(arrayOf("application/zip")) }, performHaptic = performHaptic
            )
        }
    }

    if (showBackButton) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                if (destination != SettingsDestination.Search) {
                    TopAppBar(
                        title = { Text(title, fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            if (destination != SettingsDestination.Root) {
                                IconButton(onClick = {
                                    destination = when (destination) {
                                        SettingsDestination.Language, SettingsDestination.Theme -> SettingsDestination.Appearance
                                        SettingsDestination.Search -> SettingsDestination.Root
                                        else -> SettingsDestination.Root
                                    }
                                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigation_back)) }
                            } else {
                                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigation_back)) }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    )
                }
            }
        ) { scaffoldPadding ->
            SettingsScreenContent(scaffoldPadding)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsScreenContent(PaddingValues(0.dp))
        }
    }

    SettingsDialogs(
        showProxyDialog = showProxyDialog, onDismissProxyDialog = { viewModel.setShowProxyDialog(false) }, proxyType = proxyType,
        onSelectProxyType = { settings.proxyType = it; viewModel.setShowProxyDialog(false) }, showFtAcceptDialog = showFtAcceptDialog,
        onDismissFtAcceptDialog = { viewModel.setShowFtAcceptDialog(false) }, ftAutoAccept = ftAutoAccept,
        onSelectFtAutoAccept = { settings.ftAutoAccept = it; viewModel.setShowFtAcceptDialog(false) }, showBootstrapDialog = showBootstrapDialog,
        onDismissBootstrapDialog = { viewModel.setShowBootstrapDialog(false) }, bootstrapNodeSource = bootstrapNodeSource,
        onSelectBootstrapNodeSource = { viewModel.setBootstrapNodeSource(it); viewModel.setShowBootstrapDialog(false) },
        showAccentColorDialog = showAccentColorDialog, onDismissAccentColorDialog = { showAccentColorDialog = false }, currentAccentSeed = appearance.accentColorSeed,
        onAccentColorSeedChanged = { onAccentColorSeedChanged(it); showAccentColorDialog = false }, showDateFormatDialog = showDateFormatDialog,
        onDismissDateFormatDialog = { showDateFormatDialog = false }, dateFormatPreference = dateFormatPreference,
        onSelectDateFormat = { settings.dateFormatPreference = it; showDateFormatDialog = false }, showTimeFormatDialog = showTimeFormatDialog,
        onDismissTimeFormatDialog = { showTimeFormatDialog = false }, timeFormatPreference = timeFormatPreference,
        onSelectTimeFormat = { settings.timeFormatPreference = it; showTimeFormatDialog = false }, showRestoreConfirmDialog = showRestoreConfirmDialog,
        onDismissRestoreConfirmDialog = { showRestoreConfirmDialog = false; pendingRestoreUri = null }, pendingRestoreUri = pendingRestoreUri,
        isToxStarted = backupViewModel.isToxStarted(), onRestoreConfirm = { password -> backupViewModel.restoreBackup(pendingRestoreUri!!, password); showRestoreConfirmDialog = false; pendingRestoreUri = null },
        showGoogleAccountDialog = showGoogleAccountDialog, onDismissGoogleAccountDialog = { showGoogleAccountDialog = false }, googleAccountInput = googleAccountInput,
        onGoogleAccountInputChange = { googleAccountInput = it },
        onChooseGoogleAccount = {
            try {
                val intent = android.accounts.AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
                accountPickerLauncher.launch(intent)
            } catch (e: Exception) { e.printStackTrace() }
        },
        onConfirmGoogleAccount = {
            settings.backupGoogleAccount = googleAccountInput
            showGoogleAccountDialog = false
        },
        performHaptic = performHaptic,
        focusManager = focusManager
    )
}

private fun formatSize(context: android.content.Context, bytes: Long): String {
    if (bytes <= 0) return "0 ${context.getString(R.string.size_bytes)}"
    val units = arrayOf(
        context.getString(R.string.size_bytes),
        context.getString(R.string.size_kb),
        context.getString(R.string.size_mb),
        context.getString(R.string.size_gb)
    )
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun launchRingtonePicker(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    title: String,
    type: Int,
    currentUri: String,
) {
    launcher.launch(
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                currentUri.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(type)
            )
        }
    )
}
