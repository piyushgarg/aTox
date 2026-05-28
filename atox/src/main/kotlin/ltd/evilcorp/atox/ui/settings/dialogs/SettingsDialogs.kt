// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.dialogs

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusManager
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.FtAutoAccept
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import ltd.evilcorp.atox.ui.settings.appearance.AccentColorDialog
import ltd.evilcorp.atox.ui.settings.appearance.DateFormatSettingsDialog
import ltd.evilcorp.atox.ui.settings.appearance.TimeFormatSettingsDialog
import ltd.evilcorp.atox.ui.settings.backup.GoogleAccountDialog
import ltd.evilcorp.atox.ui.settings.backup.RestoreBackupConfirmDialog
import ltd.evilcorp.atox.ui.settings.chat.FtAutoAcceptSettingsDialog
import ltd.evilcorp.atox.ui.settings.connection.BootstrapSettingsDialog
import ltd.evilcorp.atox.ui.settings.connection.ProxySettingsDialog

@Suppress("FunctionNaming")
@Composable
fun SettingsDialogs(
    showProxyDialog: Boolean,
    onDismissProxyDialog: () -> Unit,
    proxyType: ProxyType,
    onSelectProxyType: (ProxyType) -> Unit,
    showFtAcceptDialog: Boolean,
    onDismissFtAcceptDialog: () -> Unit,
    ftAutoAccept: FtAutoAccept,
    onSelectFtAutoAccept: (FtAutoAccept) -> Unit,
    showBootstrapDialog: Boolean,
    onDismissBootstrapDialog: () -> Unit,
    bootstrapNodeSource: BootstrapNodeSource,
    onSelectBootstrapNodeSource: (BootstrapNodeSource) -> Unit,
    showAccentColorDialog: Boolean,
    onDismissAccentColorDialog: () -> Unit,
    currentAccentSeed: Int,
    onAccentColorSeedChanged: (Int) -> Unit,
    showDateFormatDialog: Boolean,
    onDismissDateFormatDialog: () -> Unit,
    dateFormatPreference: DateFormatPreference,
    onSelectDateFormat: (DateFormatPreference) -> Unit,
    showTimeFormatDialog: Boolean,
    onDismissTimeFormatDialog: () -> Unit,
    timeFormatPreference: TimeFormatPreference,
    onSelectTimeFormat: (TimeFormatPreference) -> Unit,
    showRestoreConfirmDialog: Boolean,
    onDismissRestoreConfirmDialog: () -> Unit,
    pendingRestoreUri: String?,
    isToxStarted: Boolean,
    onRestoreConfirm: (String) -> Unit,
    showGoogleAccountDialog: Boolean,
    onDismissGoogleAccountDialog: () -> Unit,
    googleAccountInput: String,
    onGoogleAccountInputChange: (String) -> Unit,
    onChooseGoogleAccount: () -> Unit,
    onConfirmGoogleAccount: () -> Unit,
    performHaptic: () -> Unit,
    focusManager: FocusManager
) {
    if (showProxyDialog) {
        ProxySettingsDialog(
            currentProxyType = proxyType,
            onSelectProxyType = onSelectProxyType,
            onDismiss = onDismissProxyDialog
        )
    }

    if (showFtAcceptDialog) {
        FtAutoAcceptSettingsDialog(
            currentFtAutoAccept = ftAutoAccept,
            onSelectFtAutoAccept = onSelectFtAutoAccept,
            onDismiss = onDismissFtAcceptDialog
        )
    }

    if (showBootstrapDialog) {
        BootstrapSettingsDialog(
            currentSource = bootstrapNodeSource,
            onSelectSource = onSelectBootstrapNodeSource,
            onDismiss = onDismissBootstrapDialog
        )
    }

    if (showAccentColorDialog) {
        AccentColorDialog(
            currentAccentSeed = currentAccentSeed,
            onAccentColorSeedChanged = onAccentColorSeedChanged,
            onDismiss = onDismissAccentColorDialog
        )
    }

    if (showDateFormatDialog) {
        DateFormatSettingsDialog(
            currentFormat = dateFormatPreference,
            onSelectFormat = onSelectDateFormat,
            onDismiss = onDismissDateFormatDialog,
            performHaptic = performHaptic
        )
    }

    if (showTimeFormatDialog) {
        TimeFormatSettingsDialog(
            currentFormat = timeFormatPreference,
            onSelectFormat = onSelectTimeFormat,
            onDismiss = onDismissTimeFormatDialog,
            performHaptic = performHaptic
        )
    }

    if (showRestoreConfirmDialog && pendingRestoreUri != null) {
        RestoreBackupConfirmDialog(
            isToxStarted = isToxStarted,
            onConfirm = onRestoreConfirm,
            onDismiss = onDismissRestoreConfirmDialog,
            focusManager = focusManager
        )
    }

    if (showGoogleAccountDialog) {
        GoogleAccountDialog(
            googleAccountInput = googleAccountInput,
            onGoogleAccountInputChange = onGoogleAccountInputChange,
            onChooseAccountClick = onChooseGoogleAccount,
            onConfirm = onConfirmGoogleAccount,
            onDismiss = onDismissGoogleAccountDialog
        )
    }
}
