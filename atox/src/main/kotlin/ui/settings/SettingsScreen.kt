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
import ltd.evilcorp.atox.ui.settings.common.SettingsRootContent
import ltd.evilcorp.atox.ui.settings.common.SettingsSearchPopup
import ltd.evilcorp.atox.ui.settings.common.SettingsDestination
import ltd.evilcorp.atox.ui.settings.common.SettingsSearchIndex
import ltd.evilcorp.atox.ui.settings.backup.BackupSettingsViewModel
import ltd.evilcorp.atox.ui.settings.backup.BackupSettingsScreen
import ltd.evilcorp.atox.ui.settings.backup.RestoreBackupConfirmDialog
import ltd.evilcorp.atox.ui.settings.backup.GoogleAccountDialog
import ltd.evilcorp.atox.ui.settings.backup.BackupUiEvent
import ltd.evilcorp.atox.ui.settings.appearance.LanguageSelectionScreen
import ltd.evilcorp.atox.ui.settings.appearance.ThemeSelectionScreen
import ltd.evilcorp.atox.ui.settings.appearance.AccentColorDialog
import ltd.evilcorp.atox.ui.settings.appearance.DateFormatSettingsDialog
import ltd.evilcorp.atox.ui.settings.appearance.TimeFormatSettingsDialog
import ltd.evilcorp.atox.ui.settings.sound.SoundSettingsScreen
import ltd.evilcorp.atox.ui.settings.sound.SoundPickerTarget
import ltd.evilcorp.atox.ui.settings.connection.ProxySettingsDialog
import ltd.evilcorp.atox.ui.settings.connection.BootstrapSettingsDialog
import ltd.evilcorp.atox.ui.settings.chat.FtAutoAcceptSettingsDialog
import ltd.evilcorp.atox.ui.settings.screens.SettingsAppearanceScreen
import ltd.evilcorp.atox.ui.settings.screens.SettingsChatScreen
import ltd.evilcorp.atox.ui.settings.screens.SettingsConnectionScreen

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
        SettingsSearchIndex.buildSearchIndex(context, languages, currentLanguageCode, appThemeMode, viewModel)
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
                        performHaptic()
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
                    {
                        performHaptic()
                        destination = SettingsDestination.Search
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
            if (showBackButton && destination != SettingsDestination.Search) {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        if (destination != SettingsDestination.Root) {
                            IconButton(onClick = {
                                performHaptic()
                                destination = when (destination) {
                                    SettingsDestination.Language, SettingsDestination.Theme -> SettingsDestination.Appearance
                                    SettingsDestination.Search -> SettingsDestination.Root
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
                LanguageSelectionScreen(
                    paddingValues = paddingValues,
                    currentLanguageCode = currentLanguageCode,
                    onLanguageSelect = { localeTag ->
                        performHaptic()
                        destination = SettingsDestination.Appearance
                        onLocaleTagChanged(localeTag)
                    }
                )
            }
            SettingsDestination.Theme -> {
                ThemeSelectionScreen(
                    paddingValues = paddingValues,
                    appThemeMode = appThemeMode,
                    onThemeSelect = { themeMode ->
                        performHaptic()
                        destination = SettingsDestination.Appearance
                        onThemeChanged(themeMode)
                    }
                )
            }
            SettingsDestination.Search -> {
                SettingsSearchPopup(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    searchItems = searchItems,
                    onDismissRequest = {
                        performHaptic()
                        searchQuery = ""
                        destination = SettingsDestination.Root
                    },
                    performHaptic = performHaptic,
                    onItemClick = { item ->
                        if (item.onTrigger != null) {
                            item.onTrigger.invoke()
                        } else {
                            destination = item.destination
                        }
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



    // Dialog 3: Proxy type selection
    if (showProxyDialog) {
        ProxySettingsDialog(
            currentProxyType = proxyType,
            onSelectProxyType = {
                settings.proxyType = it
                viewModel.setShowProxyDialog(false)
            },
            onDismiss = { viewModel.setShowProxyDialog(false) }
        )
    }

    // Dialog 4: File auto-accept selection
    if (showFtAcceptDialog) {
        FtAutoAcceptSettingsDialog(
            currentFtAutoAccept = ftAutoAccept,
            onSelectFtAutoAccept = {
                settings.ftAutoAccept = it
                viewModel.setShowFtAcceptDialog(false)
            },
            onDismiss = { viewModel.setShowFtAcceptDialog(false) }
        )
    }

    // Dialog 5: Bootstrap node source selection
    if (showBootstrapDialog) {
        BootstrapSettingsDialog(
            currentSource = bootstrapNodeSource,
            onSelectSource = {
                viewModel.setBootstrapNodeSource(it)
                viewModel.setShowBootstrapDialog(false)
            },
            onDismiss = { viewModel.setShowBootstrapDialog(false) }
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
        DateFormatSettingsDialog(
            currentFormat = dateFormatPreference,
            onSelectFormat = {
                settings.dateFormatPreference = it
                showDateFormatDialog = false
            },
            onDismiss = { showDateFormatDialog = false },
            performHaptic = performHaptic
        )
    }

    if (showTimeFormatDialog) {
        TimeFormatSettingsDialog(
            currentFormat = timeFormatPreference,
            onSelectFormat = {
                settings.timeFormatPreference = it
                showTimeFormatDialog = false
            },
            onDismiss = { showTimeFormatDialog = false },
            performHaptic = performHaptic
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
        GoogleAccountDialog(
            googleAccountInput = googleAccountInput,
            onGoogleAccountInputChange = { googleAccountInput = it },
            onChooseAccountClick = {
                try {
                     val intent = android.accounts.AccountManager.newChooseAccountIntent(
                         null, null, arrayOf("com.google"), null, null, null, null
                     )
                     accountPickerLauncher.launch(intent)
                } catch (e: Exception) {
                     e.printStackTrace()
                }
            },
            onConfirm = {
                settings.backupGoogleAccount = googleAccountInput
                showGoogleAccountDialog = false
            },
            onDismiss = { showGoogleAccountDialog = false }
        )
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
