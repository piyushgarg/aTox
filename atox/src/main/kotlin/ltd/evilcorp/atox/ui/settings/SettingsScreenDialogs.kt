package ltd.evilcorp.atox.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.settings.backup.BackupSettingsViewModel
import ltd.evilcorp.atox.ui.settings.backup.GoogleDriveRestoreDialog
import ltd.evilcorp.atox.ui.settings.backup.RestoreBackupConfirmDialog
import ltd.evilcorp.atox.ui.settings.dialogs.SettingsDialogs

@Composable
internal fun SettingsScreenDialogs(
    state: SettingsScreenState,
    viewModel: SettingsViewModel,
    backupViewModel: BackupSettingsViewModel,
    settings: Settings,
    appearance: AppAppearance,
    onAccentColorSeedChanged: (Int) -> Unit,
    performHaptic: () -> Unit,
    focusManager: FocusManager
) {
    val storedSettings = settings.state.collectAsState().value
    val showProxyDialog = viewModel.showProxyDialog.collectAsState().value
    val showFtAcceptDialog = viewModel.showFtAcceptDialog.collectAsState().value
    val showBootstrapDialog = viewModel.showBootstrapDialog.collectAsState().value

    var pendingGoogleRestoreFileId by remember { mutableStateOf<String?>(null) }
    var showGoogleRestoreConfirmDialog by remember { mutableStateOf(false) }

    SettingsDialogs(
        showProxyDialog = showProxyDialog,
        onDismissProxyDialog = { viewModel.setShowProxyDialog(false) },
        proxyType = storedSettings.proxyType,
        onSelectProxyType = {
            viewModel.setProxyType(it)
            viewModel.setShowProxyDialog(false)
        },
        showFtAcceptDialog = showFtAcceptDialog,
        onDismissFtAcceptDialog = { viewModel.setShowFtAcceptDialog(false) },
        ftAutoAccept = storedSettings.ftAutoAccept,
        onSelectFtAutoAccept = {
            settings.ftAutoAccept = it
            viewModel.setShowFtAcceptDialog(false)
        },
        showBootstrapDialog = showBootstrapDialog,
        onDismissBootstrapDialog = { viewModel.setShowBootstrapDialog(false) },
        bootstrapNodeSource = storedSettings.bootstrapNodeSource,
        onSelectBootstrapNodeSource = {
            viewModel.setBootstrapNodeSource(it)
            viewModel.setShowBootstrapDialog(false)
        },
        showAccentColorDialog = state.showAccentColorDialog,
        onDismissAccentColorDialog = { state.showAccentColorDialog = false },
        currentAccentSeed = appearance.accentColorSeed,
        onAccentColorSeedChanged = {
            onAccentColorSeedChanged(it)
            state.showAccentColorDialog = false
        },
        showDateFormatDialog = state.showDateFormatDialog,
        onDismissDateFormatDialog = { state.showDateFormatDialog = false },
        dateFormatPreference = storedSettings.dateFormatPreference,
        onSelectDateFormat = {
            settings.dateFormatPreference = it
            state.showDateFormatDialog = false
        },
        showTimeFormatDialog = state.showTimeFormatDialog,
        onDismissTimeFormatDialog = { state.showTimeFormatDialog = false },
        timeFormatPreference = storedSettings.timeFormatPreference,
        onSelectTimeFormat = {
            settings.timeFormatPreference = it
            state.showTimeFormatDialog = false
        },
        showRestoreConfirmDialog = state.showRestoreConfirmDialog,
        onDismissRestoreConfirmDialog = {
            state.showRestoreConfirmDialog = false
            state.pendingRestoreUri = null
        },
        pendingRestoreUri = state.pendingRestoreUri,
        isToxStarted = backupViewModel.isToxStarted(),
        onRestoreConfirm = { password ->
            backupViewModel.restoreBackup(state.pendingRestoreUri!!, password)
            state.showRestoreConfirmDialog = false
            state.pendingRestoreUri = null
        },
        performHaptic = performHaptic,
        focusManager = focusManager
    )

    if (state.showGoogleDriveRestoreDialog) {
        val googleBackups by backupViewModel.googleBackups.collectAsState()
        GoogleDriveRestoreDialog(
            backups = googleBackups,
            onBackupSelected = { backup ->
                state.showGoogleDriveRestoreDialog = false
                pendingGoogleRestoreFileId = backup.id
                showGoogleRestoreConfirmDialog = true
            },
            onDismiss = {
                state.showGoogleDriveRestoreDialog = false
            }
        )
    }

    if (showGoogleRestoreConfirmDialog && pendingGoogleRestoreFileId != null) {
        RestoreBackupConfirmDialog(
            isToxStarted = backupViewModel.isToxStarted(),
            onConfirm = { password ->
                backupViewModel.restoreGoogleDriveBackup(pendingGoogleRestoreFileId!!, password)
                showGoogleRestoreConfirmDialog = false
                pendingGoogleRestoreFileId = null
            },
            onDismiss = {
                showGoogleRestoreConfirmDialog = false
                pendingGoogleRestoreFileId = null
            },
            focusManager = focusManager
        )
    }
}
