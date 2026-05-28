// SPDX-FileCopyrightText: 2026 aTox contributors
// SPDX-License-Identifier: GPL-3.0-only
package ltd.evilcorp.atox.ui.settings

import android.content.Intent
import android.net.Uri
import android.media.RingtoneManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.remember
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
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.settings.common.SettingsDestination
import ltd.evilcorp.atox.ui.settings.common.SettingsSearchIndex
import ltd.evilcorp.atox.ui.settings.dialogs.SettingsDialogs
import ltd.evilcorp.atox.ui.settings.backup.BackupSettingsViewModel
import ltd.evilcorp.atox.ui.settings.backup.BackupUiEvent
import ltd.evilcorp.atox.ui.settings.screens.SoundPickerTarget

@Suppress("FunctionNaming", "CyclomaticComplexMethod", "ComplexMethod", "LongMethod", "ViewModelForwarding")
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
    val defaultBackupIds = remember(backupViewModel.backupProviders) {
        backupViewModel.backupProviders.map { it.id }.toSet()
    }
    val state = rememberSettingsScreenState(
        defaultProxyPort = storedSettings.proxyPort.toString(),
        defaultBackupIds = defaultBackupIds,
        defaultCacheSize = context.getString(R.string.settings_cache_calculating)
    )
    BackHandler(enabled = state.destination != SettingsDestination.Root) {
        performHaptic()
        state.destination = when (state.destination) {
            SettingsDestination.Language, SettingsDestination.Theme -> SettingsDestination.Appearance
            SettingsDestination.Search -> SettingsDestination.Root
            else -> SettingsDestination.Root
        }
    }
    val proxyType = storedSettings.proxyType
    val proxyAddress = storedSettings.proxyAddress
    val proxyPort = storedSettings.proxyPort.toString()
    val ftAutoAccept = storedSettings.ftAutoAccept
    val bootstrapNodeSource = storedSettings.bootstrapNodeSource
    val dateFormatPreference = storedSettings.dateFormatPreference
    val timeFormatPreference = storedSettings.timeFormatPreference
    val showProxyDialog by viewModel.showProxyDialog.collectAsState()
    val showFtAcceptDialog by viewModel.showFtAcceptDialog.collectAsState()
    val showBootstrapDialog by viewModel.showBootstrapDialog.collectAsState()
    val accountPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(
                android.accounts.AccountManager.KEY_ACCOUNT_NAME
            )
            if (!accountName.isNullOrBlank()) { state.googleAccountInput = accountName }
        }
    }
    LaunchedEffect(storedSettings.backupGoogleAccount) {
        state.googleAccountInput = storedSettings.backupGoogleAccount
    }
    val mandatoryBackupId = remember(backupViewModel.backupProviders) {
        backupViewModel.backupProviders.firstOrNull()?.id.orEmpty()
    }
    val backupExporting by backupViewModel.backupExporting.collectAsState()
    val backupImporting by backupViewModel.backupImporting.collectAsState()
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            val encryptionPassword = state.backupPassword.takeIf { state.backupPasswordEnabled }
            backupViewModel.exportBackup(
                uri.toString(),
                state.selectedBackupIds + mandatoryBackupId,
                encryptionPassword
            )
        }
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            state.pendingRestoreUri = uri.toString()
            state.showRestoreConfirmDialog = true
        }
    }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pickedUri = result.data?.getParcelableExtra<Uri>(
            RingtoneManager.EXTRA_RINGTONE_PICKED_URI
        )
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            when (state.soundPickerTarget) {
                SoundPickerTarget.Sent -> settings.sentMessageSoundUri = pickedUri?.toString().orEmpty()
                SoundPickerTarget.Call -> settings.callRingtoneUri = pickedUri?.toString().orEmpty()
                SoundPickerTarget.Notification -> settings.notificationSoundUri = pickedUri?.toString().orEmpty()
                SoundPickerTarget.ActiveChat -> settings.activeChatSoundUri = pickedUri?.toString().orEmpty()
            }
        }
    }
    val autoSaveDirectoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
    LaunchedEffect(state.destination) {
        if (state.destination == SettingsDestination.Chat) {
            val sizeBytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                viewModel.getCacheSize()
            }
            state.cacheSizeText = formatSize(context, sizeBytes)
        }
    }
    val currentLanguageCode = remember(appearance.localeTag) {
        appearance.localeTag.substringBefore('-').substringBefore(',')
    }
    LaunchedEffect(storedSettings.proxyPort) {
        state.proxyPortInput = storedSettings.proxyPort.toString()
    }
    DisposableEffect(Unit) { onDispose { viewModel.commit() } }
    val systemDefaultLabel = stringResource(R.string.pref_theme_follow_system)
    val languages = remember(systemDefaultLabel) {
        listOf(
            "" to systemDefaultLabel,
            "en" to "English",
            "ru" to "Русский",
            "sv" to "Svenska",
            "de" to "Deutsch",
            "es" to "Español",
            "fr" to "Français",
            "it" to "Italiano",
            "uk" to "Українська"
        )
    }
    val searchItems = remember(languages, currentLanguageCode, appearance.themeMode) {
        SettingsSearchIndex.buildSearchIndex(
            context,
            languages,
            currentLanguageCode,
            appearance.themeMode,
            viewModel
        )
    }
    val title = when (state.destination) {
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
    LaunchedEffect(state.destination, title, showBackButton) {
        if (!showBackButton) {
            onTitleChanged(title)
            onBackActionChanged(
                if (state.destination != SettingsDestination.Root) {
                    {
                        performHaptic()
                        state.destination = when (state.destination) {
                            SettingsDestination.Language, SettingsDestination.Theme -> {
                                SettingsDestination.Appearance
                            }
                            SettingsDestination.Search -> SettingsDestination.Root
                            else -> SettingsDestination.Root
                        }
                    }
                } else null
            )
            onSearchActionChanged(
                if (state.destination == SettingsDestination.Root) {
                    { state.destination = SettingsDestination.Search }
                } else null
            )
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.uiEvents.flowWithLifecycle(lifecycleOwner.lifecycle).collect { event ->
            when (event) {
                is SettingsUiEvent.ShowToast -> {
                    Toast.makeText(
                        context,
                        context.getString(event.messageResId),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    LaunchedEffect(backupViewModel, lifecycleOwner) {
        backupViewModel.uiEvents.flowWithLifecycle(lifecycleOwner.lifecycle).collect { event ->
            when (event) {
                is BackupUiEvent.ShowToast -> {
                    Toast.makeText(
                        context,
                        context.getString(event.messageResId),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    if (showBackButton) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                SettingsScreenTopBar(
                    title = title,
                    destination = state.destination,
                    onBack = onBack,
                    performHaptic = performHaptic,
                    onDestinationChanged = { state.destination = it }
                )
            }
        ) { scaffoldPadding ->
            SettingsScreenContent(
                state = state,
                paddingValues = scaffoldPadding,
                storedSettings = storedSettings,
                appearance = appearance,
                settings = settings,
                context = context,
                languages = languages,
                currentLanguageCode = currentLanguageCode,
                timeFormatPreference = timeFormatPreference,
                dateFormatPreference = dateFormatPreference,
                performHaptic = performHaptic,
                onDynamicColorChanged = onDynamicColorChanged,
                onThemeChanged = onThemeChanged,
                onLocaleTagChanged = onLocaleTagChanged,
                autoSaveDirectoryLabel = autoSaveDirectoryLabel,
                autoSaveDirectoryLauncher = autoSaveDirectoryLauncher,
                viewModel = viewModel,
                bootstrapNodeSource = bootstrapNodeSource,
                proxyType = proxyType,
                proxyAddress = proxyAddress,
                onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                focusManager = focusManager,
                backupViewModel = backupViewModel,
                backupExporting = backupExporting,
                backupImporting = backupImporting,
                backupLauncher = backupLauncher,
                restoreBackupLauncher = restoreBackupLauncher,
                ringtonePickerLauncher = ringtonePickerLauncher,
                mandatoryBackupId = mandatoryBackupId,
                searchItems = searchItems
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsScreenContent(
                state = state,
                paddingValues = PaddingValues(0.dp),
                storedSettings = storedSettings,
                appearance = appearance,
                settings = settings,
                context = context,
                languages = languages,
                currentLanguageCode = currentLanguageCode,
                timeFormatPreference = timeFormatPreference,
                dateFormatPreference = dateFormatPreference,
                performHaptic = performHaptic,
                onDynamicColorChanged = onDynamicColorChanged,
                onThemeChanged = onThemeChanged,
                onLocaleTagChanged = onLocaleTagChanged,
                autoSaveDirectoryLabel = autoSaveDirectoryLabel,
                autoSaveDirectoryLauncher = autoSaveDirectoryLauncher,
                viewModel = viewModel,
                bootstrapNodeSource = bootstrapNodeSource,
                proxyType = proxyType,
                proxyAddress = proxyAddress,
                onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                focusManager = focusManager,
                backupViewModel = backupViewModel,
                backupExporting = backupExporting,
                backupImporting = backupImporting,
                backupLauncher = backupLauncher,
                restoreBackupLauncher = restoreBackupLauncher,
                ringtonePickerLauncher = ringtonePickerLauncher,
                mandatoryBackupId = mandatoryBackupId,
                searchItems = searchItems
            )
        }
    }
    SettingsScreenDialogs(
        state = state,
        viewModel = viewModel,
        backupViewModel = backupViewModel,
        settings = settings,
        appearance = appearance,
        onAccentColorSeedChanged = onAccentColorSeedChanged,
        performHaptic = performHaptic,
        focusManager = focusManager,
        accountPickerLauncher = accountPickerLauncher
    )
}
