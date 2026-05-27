// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ltd.evilcorp.atox.ui.settings.common.SettingsDestination
import ltd.evilcorp.atox.ui.settings.screens.SoundPickerTarget

internal class SettingsScreenState(
    destination: SettingsDestination,
    searchQuery: String,
    proxyPortInput: String,
    pendingRestoreUri: String?,
    showRestoreConfirmDialog: Boolean,
    showGoogleAccountDialog: Boolean,
    googleAccountInput: String,
    showAccentColorDialog: Boolean,
    showDateFormatDialog: Boolean,
    showTimeFormatDialog: Boolean,
    backupPasswordEnabled: Boolean,
    backupPassword: String,
    selectedBackupIds: Set<String>,
    soundPickerTarget: SoundPickerTarget,
    cacheSizeText: String,
) {
    var destination by mutableStateOf(destination)
    var searchQuery by mutableStateOf(searchQuery)
    var proxyPortInput by mutableStateOf(proxyPortInput)
    var pendingRestoreUri by mutableStateOf(pendingRestoreUri)
    var showRestoreConfirmDialog by mutableStateOf(showRestoreConfirmDialog)
    var showGoogleAccountDialog by mutableStateOf(showGoogleAccountDialog)
    var googleAccountInput by mutableStateOf(googleAccountInput)
    var showAccentColorDialog by mutableStateOf(showAccentColorDialog)
    var showDateFormatDialog by mutableStateOf(showDateFormatDialog)
    var showTimeFormatDialog by mutableStateOf(showTimeFormatDialog)
    var backupPasswordEnabled by mutableStateOf(backupPasswordEnabled)
    var backupPassword by mutableStateOf(backupPassword)
    var selectedBackupIds by mutableStateOf(selectedBackupIds)
    var soundPickerTarget by mutableStateOf(soundPickerTarget)
    var cacheSizeText by mutableStateOf(cacheSizeText)
}

@Suppress("UnstableCollections")
@Composable
internal fun rememberSettingsScreenState(
    defaultProxyPort: String,
    defaultBackupIds: Set<String>,
    defaultCacheSize: String,
) = remember {
    SettingsScreenState(
        destination = SettingsDestination.Root,
        searchQuery = "",
        proxyPortInput = defaultProxyPort,
        pendingRestoreUri = null,
        showRestoreConfirmDialog = false,
        showGoogleAccountDialog = false,
        googleAccountInput = "",
        showAccentColorDialog = false,
        showDateFormatDialog = false,
        showTimeFormatDialog = false,
        backupPasswordEnabled = false,
        backupPassword = "",
        selectedBackupIds = defaultBackupIds,
        soundPickerTarget = SoundPickerTarget.Call,
        cacheSizeText = defaultCacheSize,
    )
}
