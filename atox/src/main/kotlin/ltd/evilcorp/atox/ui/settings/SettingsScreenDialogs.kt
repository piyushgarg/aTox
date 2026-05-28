package ltd.evilcorp.atox.ui.settings

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.focus.FocusManager
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.settings.backup.BackupSettingsViewModel
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
    focusManager: FocusManager,
    accountPickerLauncher: ActivityResultLauncher<Intent>
) {
    val storedSettings = settings.state.collectAsState().value
    val showProxyDialog = viewModel.showProxyDialog.collectAsState().value
    val showFtAcceptDialog = viewModel.showFtAcceptDialog.collectAsState().value
    val showBootstrapDialog = viewModel.showBootstrapDialog.collectAsState().value

    SettingsDialogs(
        showProxyDialog = showProxyDialog,
        onDismissProxyDialog = { viewModel.setShowProxyDialog(false) },
        proxyType = storedSettings.proxyType,
        onSelectProxyType = {
            settings.proxyType = it
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
        showGoogleAccountDialog = state.showGoogleAccountDialog,
        onDismissGoogleAccountDialog = { state.showGoogleAccountDialog = false },
        googleAccountInput = state.googleAccountInput,
        onGoogleAccountInputChange = { state.googleAccountInput = it },
        onChooseGoogleAccount = {
            try {
                val intent = android.accounts.AccountManager.newChooseAccountIntent(
                    null,
                    null,
                    arrayOf("com.google"),
                    null,
                    null,
                    null,
                    null
                )
                accountPickerLauncher.launch(intent)
            } catch (e: Exception) { e.printStackTrace() }
        },
        onConfirmGoogleAccount = {
            settings.backupGoogleAccount = state.googleAccountInput
            state.showGoogleAccountDialog = false
        },
        performHaptic = performHaptic,
        focusManager = focusManager
    )
}
