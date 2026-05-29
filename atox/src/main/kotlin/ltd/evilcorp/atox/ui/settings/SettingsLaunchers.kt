// SPDX-FileCopyrightText: 2026 aTox contributors
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import android.content.Intent
import android.net.Uri
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ltd.evilcorp.atox.ui.settings.backup.BackupSettingsViewModel
import ltd.evilcorp.atox.ui.settings.screens.SoundPickerTarget

internal class SettingsLaunchers(
    val accountPickerLauncher: ActivityResultLauncher<Intent>,
    val backupLauncher: ActivityResultLauncher<String>,
    val restoreBackupLauncher: ActivityResultLauncher<Array<String>>,
    val ringtonePickerLauncher: ActivityResultLauncher<Intent>,
    val autoSaveDirectoryLauncher: ActivityResultLauncher<Uri?>,
)

@Composable
internal fun rememberSettingsLaunchers(
    state: SettingsScreenState,
    viewModel: SettingsViewModel,
    backupViewModel: BackupSettingsViewModel,
    mandatoryBackupId: String,
): SettingsLaunchers {
    val context = LocalContext.current

    val accountPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(
                android.accounts.AccountManager.KEY_ACCOUNT_NAME
            )
            if (!accountName.isNullOrBlank()) {
                state.googleAccountInput = accountName
            }
        }
    }

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
            val pickedUriStr = pickedUri?.toString().orEmpty()
            when (state.soundPickerTarget) {
                SoundPickerTarget.Sent -> viewModel.setSentMessageSoundUri(pickedUriStr)
                SoundPickerTarget.Call -> viewModel.setCallRingtoneUri(pickedUriStr)
                SoundPickerTarget.Notification -> viewModel.setNotificationSoundUri(pickedUriStr)
                SoundPickerTarget.ActiveChat -> viewModel.setActiveChatSoundUri(pickedUriStr)
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
            viewModel.setAutoSaveDirectoryUri(uri.toString())
        }
    }

    return remember(
        accountPickerLauncher,
        backupLauncher,
        restoreBackupLauncher,
        ringtonePickerLauncher,
        autoSaveDirectoryLauncher
    ) {
        SettingsLaunchers(
            accountPickerLauncher = accountPickerLauncher,
            backupLauncher = backupLauncher,
            restoreBackupLauncher = restoreBackupLauncher,
            ringtonePickerLauncher = ringtonePickerLauncher,
            autoSaveDirectoryLauncher = autoSaveDirectoryLauncher
        )
    }
}
