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

enum class GoogleSignInPurpose {
    Connect,
    Restore
}

internal class SettingsScreenState(
    destination: SettingsDestination,
    searchQuery: String,
    proxyPortInput: String,
    pendingRestoreUri: String?,
    showRestoreConfirmDialog: Boolean,
    showAccentColorDialog: Boolean,
    showDateFormatDialog: Boolean,
    showTimeFormatDialog: Boolean,
    selectedBackupIds: Set<String>,
    soundPickerTarget: SoundPickerTarget,
    cacheSizeText: String,
    showGoogleDriveRestoreDialog: Boolean,
    googleSignInPurpose: GoogleSignInPurpose,
) {
    var destination by mutableStateOf(destination)
    var searchQuery by mutableStateOf(searchQuery)
    var proxyPortInput by mutableStateOf(proxyPortInput)
    var pendingRestoreUri by mutableStateOf(pendingRestoreUri)
    var showRestoreConfirmDialog by mutableStateOf(showRestoreConfirmDialog)
    var showAccentColorDialog by mutableStateOf(showAccentColorDialog)
    var showDateFormatDialog by mutableStateOf(showDateFormatDialog)
    var showTimeFormatDialog by mutableStateOf(showTimeFormatDialog)
    var selectedBackupIds by mutableStateOf(selectedBackupIds)
    var soundPickerTarget by mutableStateOf(soundPickerTarget)
    var cacheSizeText by mutableStateOf(cacheSizeText)
    var showGoogleDriveRestoreDialog by mutableStateOf(showGoogleDriveRestoreDialog)
    var googleSignInPurpose by mutableStateOf(googleSignInPurpose)
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
        showAccentColorDialog = false,
        showDateFormatDialog = false,
        showTimeFormatDialog = false,
        selectedBackupIds = defaultBackupIds,
        soundPickerTarget = SoundPickerTarget.Call,
        cacheSizeText = defaultCacheSize,
        showGoogleDriveRestoreDialog = false,
        googleSignInPurpose = GoogleSignInPurpose.Connect,
    )
}
