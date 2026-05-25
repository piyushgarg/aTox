package ltd.evilcorp.atox.ui.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.theme.AccentPresets
import ltd.evilcorp.atox.ui.theme.LocalAToxThemeIsDark
import ltd.evilcorp.atox.ui.theme.accentPreviewColor
import ltd.evilcorp.atox.ui.theme.accentPreviewContentColor
import ltd.evilcorp.atox.ui.theme.avatarContentColor
import ltd.evilcorp.core.model.BootstrapNodeSource
import ltd.evilcorp.core.model.DateFormatPreference
import ltd.evilcorp.core.model.FtAutoAccept
import ltd.evilcorp.core.model.TimeFormatPreference
import ltd.evilcorp.core.tox.save.ProxyType
import ltd.evilcorp.atox.ui.settings.components.SettingsRootContent
import ltd.evilcorp.atox.ui.settings.components.SoundSettingsScreen
import ltd.evilcorp.atox.ui.settings.components.SoundPickerTarget
import ltd.evilcorp.atox.ui.settings.components.BackupSettingsScreen
import ltd.evilcorp.atox.ui.settings.components.RestoreBackupConfirmDialog
import ltd.evilcorp.atox.ui.settings.components.AccentColorDialog
import ltd.evilcorp.atox.ui.settings.screens.SettingsAppearanceScreen
import ltd.evilcorp.atox.ui.settings.screens.SettingsChatScreen
import ltd.evilcorp.atox.ui.settings.screens.SettingsConnectionScreen

private enum class SettingsDestination {
    Root,
    Appearance,
    Chat,
    Sounds,
    Connection,
    Backup,
    Language,
    Theme,
}

private data class SearchableSetting(
    val title: String,
    val subtitle: String,
    val destination: SettingsDestination,
    val category: String,
    val onTrigger: (() -> Unit)? = null
)

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
    vmFactory: ViewModelProvider.Factory? = null,
    onTitleChanged: (String) -> Unit = {},
    onBackActionChanged: ((() -> Unit)?) -> Unit = {},
    onSearchActionChanged: ((() -> Unit)?) -> Unit = {},
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = vmFactory)
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val backupViewModel: BackupSettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = vmFactory)
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
    var isSearchActive by remember { mutableStateOf(false) }

    val appThemeMode = appearance.themeMode
    val dynamicColor = appearance.dynamicColorEnabled
    val currentAccentSeed = appearance.accentColorSeed
    val udpEnabled = storedSettings.udpEnabled
    val runAtStartup = storedSettings.runAtStartup
    val autoAwayEnabled = storedSettings.autoAwayEnabled
    val autoAwaySeconds = storedSettings.autoAwaySeconds.toString()
    val proxyType = storedSettings.proxyType
    val proxyAddress = storedSettings.proxyAddress
    val proxyPort = storedSettings.proxyPort.toString()
    val ftAutoAccept = storedSettings.ftAutoAccept
    val bootstrapNodeSource = storedSettings.bootstrapNodeSource
    val disableScreenshots = storedSettings.disableScreenshots
    val confirmQuitting = storedSettings.confirmQuitting
    val confirmCalling = storedSettings.confirmCalling
    val sentMessageSoundVolume = storedSettings.sentMessageSoundVolume
    val callSoundVolume = storedSettings.callSoundVolume
    val callRingtoneUri = storedSettings.callRingtoneUri
    val notificationSoundVolume = storedSettings.notificationSoundVolume
    val activeChatSoundVolume = storedSettings.activeChatSoundVolume
    val sentMessageSoundUri = storedSettings.sentMessageSoundUri
    val notificationSoundUri = storedSettings.notificationSoundUri
    val activeChatSoundUri = storedSettings.activeChatSoundUri
    val dateFormatPreference = storedSettings.dateFormatPreference
    val timeFormatPreference = storedSettings.timeFormatPreference
    var autoAwaySecondsInput by remember { mutableStateOf(autoAwaySeconds) }
    var proxyPortInput by remember { mutableStateOf(proxyPort) }

    val showProxyDialog by viewModel.showProxyDialog.collectAsState()
    val showFtAcceptDialog by viewModel.showFtAcceptDialog.collectAsState()
    val showBootstrapDialog by viewModel.showBootstrapDialog.collectAsState()
    var pendingRestoreUri by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showGoogleAccountDialog by remember { mutableStateOf(false) }
    var googleAccountInput by remember { mutableStateOf("") }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                googleAccountInput = accountName
            }
        }
    }

    LaunchedEffect(storedSettings.backupGoogleAccount) {
        googleAccountInput = storedSettings.backupGoogleAccount
    }
    var showAccentColorDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }
    var backupPasswordEnabled by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    val mandatoryBackupId = remember(backupViewModel.backupProviders) { backupViewModel.backupProviders.firstOrNull()?.id.orEmpty() }
    var selectedBackupIds by remember(backupViewModel.backupProviders) {
        mutableStateOf(backupViewModel.backupProviders.map { it.id }.toSet())
    }
    var soundPickerTarget by remember { mutableStateOf(SoundPickerTarget.Call) }
    val backupExporting by backupViewModel.backupExporting.collectAsState()
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            backupViewModel.exportBackup(
                uriString = uri.toString(),
                selectedIds = selectedBackupIds + mandatoryBackupId,
                password = backupPassword.takeIf { backupPasswordEnabled },
            )
        }
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri.toString()
            showRestoreConfirmDialog = true
        }
    }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pickedUri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            when (soundPickerTarget) {
                SoundPickerTarget.Sent -> settings.sentMessageSoundUri = pickedUri?.toString().orEmpty()
                SoundPickerTarget.Call -> settings.callRingtoneUri = pickedUri?.toString().orEmpty()
                SoundPickerTarget.Notification -> settings.notificationSoundUri = pickedUri?.toString().orEmpty()
                SoundPickerTarget.ActiveChat -> settings.activeChatSoundUri = pickedUri?.toString().orEmpty()
            }
            performHaptic()
        }
    }

    val autoSaveDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
            settings.autoSaveDirectoryUri = uri.toString()
            performHaptic()
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
                val parts = docId.split(":")
                if (parts.size > 1) {
                    parts[1]
                } else {
                    docId
                }
            }.getOrElse {
                uriString.substringAfterLast("/")
            }.takeIf { it.isNotBlank() } ?: "Folder"
        }
    }

    var cacheSizeText by remember { mutableStateOf(context.getString(R.string.settings_cache_calculating)) }
    LaunchedEffect(destination) {
        if (destination == SettingsDestination.Chat) {
            val sizeBytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                viewModel.getCacheSize()
            }
            cacheSizeText = formatSize(context, sizeBytes)
        }
    }

    val currentLanguageCode = remember(appearance.localeTag) {
        appearance.localeTag.substringBefore('-').substringBefore(',')
    }

    LaunchedEffect(storedSettings.autoAwaySeconds) {
        autoAwaySecondsInput = storedSettings.autoAwaySeconds.toString()
    }

    LaunchedEffect(storedSettings.proxyPort) {
        proxyPortInput = storedSettings.proxyPort.toString()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.commit()
        }
    }

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

    val searchItems = remember(languages, currentLanguageCode, appThemeMode) {
        listOf(
            SearchableSetting(
                title = context.getString(R.string.language),
                subtitle = languages.find { it.first == currentLanguageCode }?.second ?: "English",
                destination = SettingsDestination.Language,
                category = context.getString(R.string.appearance_and_design)
            ),
            SearchableSetting(
                title = context.getString(R.string.pref_heading_theme),
                subtitle = when (appThemeMode) {
                    AppCompatDelegate.MODE_NIGHT_YES -> context.getString(R.string.pref_theme_dark)
                    AppCompatDelegate.MODE_NIGHT_NO -> context.getString(R.string.pref_theme_light)
                    else -> context.getString(R.string.pref_theme_follow_system)
                },
                destination = SettingsDestination.Theme,
                category = context.getString(R.string.appearance_and_design)
            ),
            SearchableSetting(
                title = context.getString(R.string.settings_sounds_group),
                subtitle = context.getString(R.string.settings_sounds_summary),
                destination = SettingsDestination.Sounds,
                category = context.getString(R.string.settings_sounds_group)
            ),
            SearchableSetting(
                title = context.getString(R.string.backup_title),
                subtitle = context.getString(R.string.backup_settings_subtitle),
                destination = SettingsDestination.Backup,
                category = context.getString(R.string.settings_privacy_group)
            ),
            SearchableSetting(
                title = context.getString(R.string.settings_clear_cache_title),
                subtitle = context.getString(R.string.settings_clear_cache_title),
                destination = SettingsDestination.Root,
                category = context.getString(R.string.settings_storage_group)
            ),
            SearchableSetting(
                title = context.getString(R.string.settings_proxy_type),
                subtitle = context.getString(R.string.settings_proxy_type),
                destination = SettingsDestination.Root,
                category = context.getString(R.string.settings_proxy_group),
                onTrigger = { viewModel.setShowProxyDialog(true) }
            )
        )
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
    }

    LaunchedEffect(destination, title, showBackButton) {
        if (!showBackButton) {
            onTitleChanged(title)
            onBackActionChanged(
                if (destination != SettingsDestination.Root) {
                    {
                        performHaptic()
                        destination = when (destination) {
                            SettingsDestination.Language, SettingsDestination.Theme -> SettingsDestination.Appearance
                            else -> SettingsDestination.Root
                        }
                    }
                } else null
            )
            onSearchActionChanged(
                if (destination == SettingsDestination.Root) {
                    {
                        performHaptic()
                        isSearchActive = true
                    }
                } else null
            )
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.uiEvents
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collect { event ->
                when (event) {
                    is SettingsUiEvent.ShowToast -> {
                        Toast.makeText(context, context.getString(event.messageResId), Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    LaunchedEffect(backupViewModel, lifecycleOwner) {
        backupViewModel.uiEvents
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collect { event ->
                when (event) {
                    is BackupUiEvent.ShowToast -> {
                        Toast.makeText(context, context.getString(event.messageResId), Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (showBackButton) {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        if (destination != SettingsDestination.Root) {
                            IconButton(onClick = {
                                performHaptic()
                                destination = when (destination) {
                                    SettingsDestination.Language, SettingsDestination.Theme -> SettingsDestination.Appearance
                                    else -> SettingsDestination.Root
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = {
                                performHaptic()
                                onBack()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }
    ) { scaffoldPadding ->
        val paddingValues = if (showBackButton) scaffoldPadding else PaddingValues(0.dp)
        when (destination) {
            SettingsDestination.Root -> {
                SettingsRootContent(
                    paddingValues = paddingValues,
                    currentLanguageLabel = languages.find { it.first == currentLanguageCode }?.second ?: "English",
                    themeLabel = when (appThemeMode) {
                        AppCompatDelegate.MODE_NIGHT_YES -> stringResource(R.string.pref_theme_dark)
                        AppCompatDelegate.MODE_NIGHT_NO -> stringResource(R.string.pref_theme_light)
                        else -> stringResource(R.string.pref_theme_follow_system)
                    },
                    onAppearanceClick = {
                        performHaptic()
                        destination = SettingsDestination.Appearance
                    },
                    onChatClick = {
                        performHaptic()
                        destination = SettingsDestination.Chat
                    },
                    onSoundsClick = {
                        performHaptic()
                        destination = SettingsDestination.Sounds
                    },
                    onConnectionClick = {
                        performHaptic()
                        destination = SettingsDestination.Connection
                    },
                    onBackupClick = {
                        performHaptic()
                        destination = SettingsDestination.Backup
                    }
                )
            }
            SettingsDestination.Appearance -> {
                SettingsAppearanceScreen(
                    paddingValues = paddingValues,
                    currentLanguageCode = currentLanguageCode,
                    languages = languages,
                    appThemeMode = appThemeMode,
                    timeFormatPreference = timeFormatPreference,
                    dateFormatPreference = dateFormatPreference,
                    dynamicColor = dynamicColor,
                    currentAccentSeed = currentAccentSeed,
                    hapticEnabled = storedSettings.hapticEnabled,
                    performHaptic = performHaptic,
                    onLanguageClick = {
                        performHaptic()
                        destination = SettingsDestination.Language
                    },
                    onThemeClick = {
                        performHaptic()
                        destination = SettingsDestination.Theme
                    },
                    onDateFormatClick = {
                        performHaptic()
                        showDateFormatDialog = true
                    },
                    onTimeFormatClick = {
                        performHaptic()
                        showTimeFormatDialog = true
                    },
                    onDynamicColorChanged = { checked ->
                        performHaptic()
                        onDynamicColorChanged(checked)
                    },
                    onAccentColorClick = {
                        performHaptic()
                        showAccentColorDialog = true
                    },
                    onHapticEnabledChanged = { checked ->
                        settings.hapticEnabled = checked
                        performHaptic()
                    }
                )
            }
            SettingsDestination.Chat -> {
                SettingsChatScreen(
                    paddingValues = paddingValues,
                    ftAutoAccept = ftAutoAccept,
                    autoSaveToDownloads = storedSettings.autoSaveToDownloads,
                    autoSaveDirectoryLabel = autoSaveDirectoryLabel,
                    cacheSizeText = cacheSizeText,
                    enableReplies = storedSettings.enableReplies,
                    performHaptic = performHaptic,
                    onFtAutoAcceptClick = {
                        performHaptic()
                        viewModel.setShowFtAcceptDialog(true)
                    },
                    onAutoSaveToDownloadsChanged = { checked ->
                        performHaptic()
                        settings.autoSaveToDownloads = checked
                    },
                    onAutoSaveDirectoryClick = {
                        performHaptic()
                        autoSaveDirectoryLauncher.launch(null)
                    },
                    onClearCacheClick = {
                        performHaptic()
                        viewModel.clearCache()
                        cacheSizeText = formatSize(context, 0)
                    },
                    onEnableRepliesChanged = { checked ->
                        performHaptic()
                        settings.enableReplies = checked
                    }
                )
            }
            SettingsDestination.Connection -> {
                SettingsConnectionScreen(
                    paddingValues = paddingValues,
                    udpEnabled = udpEnabled,
                    runAtStartup = runAtStartup,
                    bootstrapNodeSource = bootstrapNodeSource,
                    disableScreenshots = disableScreenshots,
                    confirmQuitting = confirmQuitting,
                    confirmCalling = confirmCalling,
                    proxyType = proxyType,
                    proxyAddress = proxyAddress,
                    proxyPortInput = proxyPortInput,
                    focusManager = focusManager,
                    performHaptic = performHaptic,
                    onUdpEnabledChanged = { checked ->
                        viewModel.setUdpEnabled(checked)
                    },
                    onRunAtStartupChanged = { checked ->
                        viewModel.setRunAtStartup(checked)
                    },
                    onBootstrapNodesClick = {
                        viewModel.setShowBootstrapDialog(true)
                    },
                    onDisableScreenshotsChanged = { checked ->
                        onDisableScreenshotsChanged(checked)
                    },
                    onConfirmQuittingChanged = { checked ->
                        settings.confirmQuitting = checked
                    },
                    onConfirmCallingChanged = { checked ->
                        settings.confirmCalling = checked
                    },
                    onProxyTypeClick = {
                        viewModel.setShowProxyDialog(true)
                    },
                    onProxyAddressChanged = {
                        settings.proxyAddress = it
                    },
                    onProxyPortInputChanged = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            proxyPortInput = it
                            viewModel.setProxyPortString(it)
                        }
                    }
                )
            }
            SettingsDestination.Language -> {
                SelectionScreen(
                    paddingValues = paddingValues,
                    items = languages,
                    selectedKey = currentLanguageCode,
                    onSelect = { localeTag ->
                        performHaptic()
                        destination = SettingsDestination.Appearance
                        onLocaleTagChanged(localeTag)
                    }
                )
            }
            SettingsDestination.Theme -> {
                val themes = listOf(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM to stringResource(R.string.pref_theme_follow_system),
                    AppCompatDelegate.MODE_NIGHT_NO to stringResource(R.string.pref_theme_light),
                    AppCompatDelegate.MODE_NIGHT_YES to stringResource(R.string.pref_theme_dark)
                )
                SelectionScreen(
                    paddingValues = paddingValues,
                    items = themes,
                    selectedKey = appThemeMode,
                    onSelect = { themeMode ->
                        performHaptic()
                        destination = SettingsDestination.Appearance
                        onThemeChanged(themeMode)
                    }
                )
            }
            SettingsDestination.Sounds -> {
                SoundSettingsScreen(
                    paddingValues = paddingValues,
                    sentMessageSoundVolume = sentMessageSoundVolume,
                    callSoundVolume = callSoundVolume,
                    notificationSoundVolume = notificationSoundVolume,
                    activeChatSoundVolume = activeChatSoundVolume,
                    sentMessageSoundUri = sentMessageSoundUri,
                    callRingtoneUri = callRingtoneUri,
                    notificationSoundUri = notificationSoundUri,
                    activeChatSoundUri = activeChatSoundUri,
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
                        launchRingtonePicker(
                            launcher = ringtonePickerLauncher,
                            title = when (target) {
                                SoundPickerTarget.Sent -> context.getString(R.string.settings_sent_sound_title)
                                SoundPickerTarget.Call -> context.getString(R.string.settings_call_sound_title)
                                SoundPickerTarget.Notification -> context.getString(R.string.settings_notification_sound_title)
                                SoundPickerTarget.ActiveChat -> context.getString(R.string.settings_active_chat_sound_title)
                            },
                            type = type,
                            currentUri = currentUri
                        )
                    },
                    performHaptic = performHaptic
                )
            }
            SettingsDestination.Backup -> {
                val backupFrequency = storedSettings.backupFrequency
                val backupUseCellular = storedSettings.backupUseCellular
                val backupDestinations = settings.backupDestinations
                val backupEndToEndEncryptionEnabled = storedSettings.backupEndToEndEncryptionEnabled
                val backupGoogleAccount = storedSettings.backupGoogleAccount
                val backupImporting by backupViewModel.backupImporting.collectAsState()

                BackupSettingsScreen(
                    paddingValues = paddingValues,
                    backupProviders = backupViewModel.backupProviders,
                    backupExporting = backupExporting,
                    backupImporting = backupImporting,
                    backupPasswordEnabled = backupPasswordEnabled,
                    backupPassword = backupPassword,
                    backupPasswordVisible = false,
                    automaticBackupEnabled = storedSettings.automaticBackupEnabled,
                    backupFrequency = backupFrequency,
                    backupUseCellular = backupUseCellular,
                    backupDestinations = backupDestinations,
                    backupEndToEndEncryptionEnabled = backupEndToEndEncryptionEnabled,
                    backupGoogleAccount = backupGoogleAccount,
                    selectedBackupIds = selectedBackupIds,
                    mandatoryBackupId = mandatoryBackupId,
                    onBackupPasswordEnabledChanged = { backupPasswordEnabled = it },
                    onBackupPasswordChanged = { backupPassword = it },
                    onBackupPasswordVisibleChanged = { /* unused */ },
                    onAutomaticBackupEnabledChanged = { settings.automaticBackupEnabled = it },
                    onBackupFrequencyChanged = { viewModel.setBackupFrequency(it) },
                    onBackupUseCellularChanged = { settings.backupUseCellular = it },
                    onBackupDestinationsChanged = { viewModel.setBackupDestinations(it) },
                    onBackupEndToEndEncryptionEnabledChanged = { settings.backupEndToEndEncryptionEnabled = it },
                    onGoogleAccountClick = { showGoogleAccountDialog = true },
                    onSelectedBackupIdsChanged = { selectedBackupIds = it },
                    onCreateBackupClick = {
                        backupLauncher.launch("atox-backup.zip")
                    },
                    onRestoreBackupClick = {
                        restoreBackupLauncher.launch(arrayOf("application/zip"))
                    },
                    performHaptic = performHaptic
                )
            }
        }
    }

    if (isSearchActive) {
        Popup(
            onDismissRequest = {
                searchQuery = ""
                isSearchActive = false
            },
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                isSearchActive = false
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_settings)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                    
                    HorizontalDivider()
                    
                    val filteredItems = remember(searchQuery, searchItems) {
                        if (searchQuery.isBlank()) {
                            emptyList()
                        } else {
                            searchItems.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.subtitle.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredItems.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (searchQuery.isBlank()) {
                                            stringResource(R.string.search_settings)
                                        } else {
                                            stringResource(R.string.search_no_results)
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(filteredItems) { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            performHaptic()
                                            searchQuery = ""
                                            isSearchActive = false
                                            if (item.onTrigger != null) {
                                                item.onTrigger.invoke()
                                            } else {
                                                destination = item.destination
                                            }
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog 3: Proxy type selection
    if (showProxyDialog) {
        val proxyTypes = listOf(
            ProxyType.None to stringResource(R.string.pref_proxy_type_none),
            ProxyType.HTTP to stringResource(R.string.pref_proxy_type_http),
            ProxyType.SOCKS5 to stringResource(R.string.pref_proxy_type_socks5)
        )
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { viewModel.setShowProxyDialog(false) },
            title = { Text(stringResource(R.string.settings_proxy_type), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    proxyTypes.forEach { item ->
                        val isSelected = item.first == proxyType
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    settings.proxyType = item.first
                                    viewModel.setShowProxyDialog(false)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.second,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowProxyDialog(false) }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Dialog 4: File auto-accept selection
    if (showFtAcceptDialog) {
        val ftTypes = listOf(
            FtAutoAccept.None to stringResource(R.string.pref_ft_auto_accept_none),
            FtAutoAccept.Images to stringResource(R.string.pref_ft_auto_accept_images),
            FtAutoAccept.All to stringResource(R.string.pref_ft_auto_accept_all)
        )
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { viewModel.setShowFtAcceptDialog(false) },
            title = { Text(stringResource(R.string.pref_heading_ft_auto_accept), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ftTypes.forEach { item ->
                        val isSelected = item.first == ftAutoAccept
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    settings.ftAutoAccept = item.first
                                    viewModel.setShowFtAcceptDialog(false)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.second,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowFtAcceptDialog(false) }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Dialog 5: Bootstrap node source selection
    if (showBootstrapDialog) {
        val bootstrapTypes = listOf(
            BootstrapNodeSource.BuiltIn to stringResource(R.string.settings_nodes_builtin),
            BootstrapNodeSource.UserProvided to stringResource(R.string.settings_nodes_user)
        )
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { viewModel.setShowBootstrapDialog(false) },
            title = { Text(stringResource(R.string.settings_nodes_list), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bootstrapTypes.forEach { item ->
                        val isSelected = item.first == bootstrapNodeSource
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    settings.bootstrapNodeSource = item.first
                                    viewModel.setShowBootstrapDialog(false)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.second,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowBootstrapDialog(false) }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Dialog 6: Accent color selection
    if (showAccentColorDialog) {
        AccentColorDialog(
            currentAccentSeed = currentAccentSeed,
            onAccentColorSeedChanged = { seed ->
                onAccentColorSeedChanged(seed)
                showAccentColorDialog = false
            },
            onDismiss = { showAccentColorDialog = false }
        )
    }

    if (showDateFormatDialog) {
        val dateFormats = listOf(
            DateFormatPreference.System to stringResource(R.string.settings_date_format_system),
            DateFormatPreference.DMY to stringResource(R.string.settings_date_format_dmy),
            DateFormatPreference.DMYDots to stringResource(R.string.settings_date_format_dmy_dots),
            DateFormatPreference.MDY to stringResource(R.string.settings_date_format_mdy),
            DateFormatPreference.YMD to stringResource(R.string.settings_date_format_ymd)
        )
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { showDateFormatDialog = false },
            title = { Text(stringResource(R.string.settings_date_format_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dateFormats.forEach { item ->
                        val isSelected = item.first == dateFormatPreference
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    performHaptic()
                                    settings.dateFormatPreference = item.first
                                    showDateFormatDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.second,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDateFormatDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showTimeFormatDialog) {
        val timeFormats = listOf(
            TimeFormatPreference.System to stringResource(R.string.settings_time_format_system),
            TimeFormatPreference.Hours24 to stringResource(R.string.settings_time_format_24h),
            TimeFormatPreference.Hours12 to stringResource(R.string.settings_time_format_12h)
        )
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { showTimeFormatDialog = false },
            title = { Text(stringResource(R.string.settings_time_format_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timeFormats.forEach { item ->
                        val isSelected = item.first == timeFormatPreference
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    performHaptic()
                                    settings.timeFormatPreference = item.first
                                    showTimeFormatDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.second,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeFormatDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showRestoreConfirmDialog && pendingRestoreUri != null) {
        RestoreBackupConfirmDialog(
            isToxStarted = backupViewModel.isToxStarted(),
            onConfirm = { password ->
                backupViewModel.restoreBackup(pendingRestoreUri!!, password)
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            onDismiss = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            focusManager = focusManager
        )
    }

    if (showGoogleAccountDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { showGoogleAccountDialog = false },
            title = { Text(stringResource(R.string.backup_google_account)) },
            text = {
                OutlinedTextField(
                    value = googleAccountInput,
                    onValueChange = { googleAccountInput = it },
                    label = { Text("Email") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            try {
                                val intent = android.accounts.AccountManager.newChooseAccountIntent(
                                    null, null, arrayOf("com.google"), null, null, null, null
                                )
                                accountPickerLauncher.launch(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Choose account"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.backupGoogleAccount = googleAccountInput
                    showGoogleAccountDialog = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleAccountDialog = false }) {
                    Text(stringResource(R.string.reject))
                }
            }
        )
    }

}

@Composable
private fun <T> SelectionScreen(
    paddingValues: PaddingValues,
    items: List<Pair<T, String>>,
    selectedKey: T,
    onSelect: (T) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column {
                    items.forEachIndexed { index, item ->
                        SelectionRow(
                            title = item.second,
                            selected = item.first == selectedKey,
                            onClick = { onSelect(item.first) },
                        )
                        if (index != items.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        RadioButton(
            selected = selected,
            onClick = null,
        )
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SoundUriRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) = SettingsClickableRow(title, subtitle, onClick)

@Composable
private fun BackupModuleCard(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

private fun soundTitle(context: android.content.Context, uriString: String, type: Int): String {
    val uri = uriString.takeIf { it.isNotBlank() }?.let(Uri::parse)
        ?: RingtoneManager.getDefaultUri(type)
    return RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        ?: context.getString(R.string.settings_call_sound_default)
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
                currentUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
                    ?: RingtoneManager.getDefaultUri(type)
            )
        }
    )
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = title, 
                style = MaterialTheme.typography.bodyLarge, 
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: () -> Unit = {},
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

@Composable
fun AccentColorSelector(
    currentSeed: Int,
    onSeedSelected: (Int) -> Unit
) {
    val isDarkTheme = LocalAToxThemeIsDark.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.settings_accent_dialog_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccentPresets.forEach { preset ->
                val isSelected = preset.seed.value.toInt() == currentSeed
                val previewColor = remember(preset.seed, isDarkTheme) {
                    accentPreviewColor(preset.seed.toArgb(), isDarkTheme)
                }
                val previewContentColor = remember(preset.seed, isDarkTheme) {
                    accentPreviewContentColor(preset.seed.toArgb(), isDarkTheme)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                        .clickable {
                            onSeedSelected(preset.seed.value.toInt())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = avatarContentColor(previewColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
     }
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
