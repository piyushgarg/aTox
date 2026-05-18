package ltd.evilcorp.atox.ui.settings

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
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
import ltd.evilcorp.core.model.FtAutoAccept
import ltd.evilcorp.core.tox.save.ProxyType

private enum class SettingsDestination {
    Root,
    Language,
    Theme,
}

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
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = vmFactory)
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storedSettings by settings.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val performHaptic = {
        if (storedSettings.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    var destination by remember { mutableStateOf(SettingsDestination.Root) }

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
    var autoAwaySecondsInput by remember { mutableStateOf(autoAwaySeconds) }
    var proxyPortInput by remember { mutableStateOf(proxyPort) }

    var showProxyDialog by remember { mutableStateOf(false) }
    var showFtAcceptDialog by remember { mutableStateOf(false) }
    var showBootstrapDialog by remember { mutableStateOf(false) }
    var showAccentColorDialog by remember { mutableStateOf(false) }

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

    val title = when (destination) {
        SettingsDestination.Root -> stringResource(R.string.settings)
        SettingsDestination.Language -> stringResource(R.string.select_language)
        SettingsDestination.Theme -> stringResource(R.string.settings_app_theme_dialog_title)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (destination != SettingsDestination.Root) {
                        IconButton(onClick = { destination = SettingsDestination.Root }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        when (destination) {
            SettingsDestination.Root -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                ) {
                    item {
                        SettingsGroup(title = stringResource(R.string.appearance_and_design)) {
                    // Language selection
                    SettingsClickableRow(
                        title = stringResource(R.string.language),
                        subtitle = languages.find { it.first == currentLanguageCode }?.second ?: "English"
                    ) {
                        performHaptic()
                        destination = SettingsDestination.Language
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Theme selection
                    val themeLabel = when (appThemeMode) {
                        AppCompatDelegate.MODE_NIGHT_YES -> stringResource(R.string.pref_theme_dark)
                        AppCompatDelegate.MODE_NIGHT_NO -> stringResource(R.string.pref_theme_light)
                        else -> stringResource(R.string.pref_theme_follow_system)
                    }
                    SettingsClickableRow(
                        title = stringResource(R.string.pref_heading_theme),
                        subtitle = themeLabel
                    ) {
                        performHaptic()
                        destination = SettingsDestination.Theme
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Dynamic Colors
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val dynamicSubtitle = stringResource(R.string.settings_dynamic_theme_subtitle)
                        SettingsSwitchRow(
                            title = stringResource(R.string.dynamic_theme),
                            subtitle = dynamicSubtitle,
                            checked = dynamicColor
                        ) { checked ->
                            performHaptic()
                            onDynamicColorChanged(checked)
                        }

                        if (!dynamicColor) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            val activePreset = AccentPresets.find { it.seed.toArgb() == currentAccentSeed } ?: AccentPresets[0]
                            SettingsClickableRow(
                                title = stringResource(R.string.accent_color),
                                subtitle = activePreset.name
                            ) {
                                performHaptic()
                                showAccentColorDialog = true
                            }
                        }
                    } else {
                        val activePreset = AccentPresets.find { it.seed.toArgb() == currentAccentSeed } ?: AccentPresets[0]
                        SettingsClickableRow(
                            title = stringResource(R.string.accent_color),
                            subtitle = activePreset.name
                        ) {
                            performHaptic()
                            showAccentColorDialog = true
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    SettingsSwitchRow(
                        title = stringResource(R.string.pref_haptic_title),
                        subtitle = stringResource(R.string.pref_haptic_summary),
                        checked = storedSettings.hapticEnabled
                    ) { checked ->
                        settings.hapticEnabled = checked
                        performHaptic()
                    }
                }
            }

                    item {
                        SettingsGroup(title = stringResource(R.string.settings_privacy_group)) {
                    // Disable screenshots
                    SettingsSwitchRow(
                        title = stringResource(R.string.pref_block_screenshots),
                        subtitle = stringResource(R.string.pref_block_screenshots_description),
                        checked = disableScreenshots
                    ) { checked ->
                        onDisableScreenshotsChanged(checked)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Confirm quitting
                    SettingsSwitchRow(
                        title = stringResource(R.string.pref_confirm_quitting),
                        subtitle = stringResource(R.string.quit_confirm),
                        checked = confirmQuitting
                    ) { checked ->
                        settings.confirmQuitting = checked
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Confirm calling
                    SettingsSwitchRow(
                        title = stringResource(R.string.pref_confirm_calling),
                        subtitle = stringResource(R.string.call_confirm),
                        checked = confirmCalling
                    ) { checked ->
                        settings.confirmCalling = checked
                    }
                }
            }

                    item {
                        SettingsGroup(title = stringResource(R.string.settings_network_group)) {
                    // UDP соединения
                    SettingsSwitchRow(
                        title = stringResource(R.string.pref_udp_enabled),
                        subtitle = stringResource(R.string.pref_udp_enabled),
                        checked = udpEnabled
                    ) { checked ->
                        viewModel.setUdpEnabled(checked)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Run at startup
                    SettingsSwitchRow(
                        title = stringResource(R.string.pref_run_at_startup),
                        subtitle = stringResource(R.string.settings_start_on_boot_sub),
                        checked = runAtStartup
                    ) { checked ->
                        viewModel.setRunAtStartup(checked)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Bootstrap Node Source
                    val bootstrapLabel = when (bootstrapNodeSource) {
                        BootstrapNodeSource.BuiltIn -> stringResource(R.string.settings_nodes_builtin)
                        BootstrapNodeSource.UserProvided -> stringResource(R.string.settings_nodes_user)
                    }
                    SettingsClickableRow(
                        title = stringResource(R.string.settings_nodes_list),
                        subtitle = bootstrapLabel
                    ) {
                        showBootstrapDialog = true
                    }
                }
            }

                    item {
                        SettingsGroup(title = stringResource(R.string.settings_ft_group)) {
                            val ftLabel = when (ftAutoAccept) {
                                FtAutoAccept.None -> stringResource(R.string.pref_ft_auto_accept_none)
                                FtAutoAccept.Images -> stringResource(R.string.pref_ft_auto_accept_images)
                                FtAutoAccept.All -> stringResource(R.string.pref_ft_auto_accept_all)
                            }
                            SettingsClickableRow(
                                title = stringResource(R.string.pref_heading_ft_auto_accept),
                                subtitle = ftLabel
                            ) {
                                showFtAcceptDialog = true
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_auto_save_title),
                                subtitle = stringResource(R.string.settings_auto_save_subtitle),
                                checked = storedSettings.autoSaveToDownloads
                            ) { checked ->
                                settings.autoSaveToDownloads = checked
                            }
                        }
                    }

                    item {
                        SettingsGroup(title = stringResource(R.string.settings_storage_group)) {
                            var cacheSizeText by remember { mutableStateOf(context.getString(R.string.settings_cache_calculating)) }

                            LaunchedEffect(Unit) {
                                val sizeBytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    viewModel.getCacheSize()
                                }
                                cacheSizeText = formatSize(context, sizeBytes)
                            }

                            SettingsClickableRow(
                                title = stringResource(R.string.settings_clear_cache_title),
                                subtitle = stringResource(R.string.settings_clear_cache_subtitle, cacheSizeText)
                            ) {
                                performHaptic()
                                viewModel.clearCache()
                                cacheSizeText = formatSize(context, 0)
                            }
                        }
                    }

                    item {
                        SettingsGroup(title = stringResource(R.string.settings_proxy_group)) {
                    val proxyLabel = when (proxyType) {
                        ProxyType.None -> stringResource(R.string.pref_proxy_type_none)
                        ProxyType.HTTP -> stringResource(R.string.pref_proxy_type_http)
                        ProxyType.SOCKS5 -> stringResource(R.string.pref_proxy_type_socks5)
                    }
                    SettingsClickableRow(
                        title = stringResource(R.string.settings_proxy_type),
                        subtitle = proxyLabel
                    ) {
                        showProxyDialog = true
                    }

                    if (proxyType != ProxyType.None) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = proxyAddress,
                                onValueChange = {
                                    settings.proxyAddress = it
                                },
                                label = { Text(stringResource(R.string.settings_proxy_address)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = proxyPortInput,
                                onValueChange = {
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        proxyPortInput = it
                                        if (it.isNotEmpty()) {
                                            settings.proxyPort = it.toIntOrNull() ?: 0
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.settings_proxy_port)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

                    item {
                        SettingsGroup(title = stringResource(R.string.settings_status_group)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.pref_auto_away),
                        subtitle = stringResource(R.string.settings_auto_away_sub),
                        checked = autoAwayEnabled
                    ) { checked ->
                        settings.autoAwayEnabled = checked
                    }

                    if (autoAwayEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = autoAwaySecondsInput,
                                onValueChange = {
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        autoAwaySecondsInput = it
                                        if (it.isNotEmpty()) {
                                            settings.autoAwaySeconds = it.toLongOrNull() ?: 180L
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.settings_auto_away_timeout)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
                }
            }
            SettingsDestination.Language -> {
                SelectionScreen(
                    paddingValues = paddingValues,
                    items = languages,
                    selectedKey = currentLanguageCode,
                    onSelect = { localeTag ->
                        performHaptic()
                        destination = SettingsDestination.Root
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
                        destination = SettingsDestination.Root
                        onThemeChanged(themeMode)
                    }
                )
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
            onDismissRequest = { showProxyDialog = false },
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
                                    showProxyDialog = false
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
                TextButton(onClick = { showProxyDialog = false }) {
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
            onDismissRequest = { showFtAcceptDialog = false },
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
                                    showFtAcceptDialog = false
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
                TextButton(onClick = { showFtAcceptDialog = false }) {
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
            onDismissRequest = { showBootstrapDialog = false },
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
                                    showBootstrapDialog = false
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
                TextButton(onClick = { showBootstrapDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Dialog 6: Accent color selection
    if (showAccentColorDialog) {
        val isDarkTheme = LocalAToxThemeIsDark.current
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { showAccentColorDialog = false },
            title = { Text(stringResource(R.string.accent_preset), fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                ) {
                    items(AccentPresets.size) { index ->
                        val preset = AccentPresets[index]
                        val isSelected = preset.seed.toArgb() == currentAccentSeed
                        val previewColor = remember(preset.seed, isDarkTheme) {
                            accentPreviewColor(preset.seed.toArgb(), isDarkTheme)
                        }
                        val previewContentColor = remember(preset.seed, isDarkTheme) {
                            accentPreviewContentColor(preset.seed.toArgb(), isDarkTheme)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    performHaptic()
                                    val seed = preset.seed.toArgb()
                                    onAccentColorSeedChanged(seed)
                                    showAccentColorDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(previewColor)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = preset.name,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAccentColorDialog = false
                }) {
                    Text(stringResource(R.string.close))
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
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { isExpanded = !isExpanded }
                .animateContentSize()
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
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
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
