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
import ltd.evilcorp.atox.ui.settings.screens.SoundPickerTarget
import ltd.evilcorp.atox.ui.settings.backup.BackupSettingsViewModel

internal class SettingsLaunchers(
    val restoreBackupLauncher: ActivityResultLauncher<Array<String>>,
    val ringtonePickerLauncher: ActivityResultLauncher<Intent>,
    val autoSaveDirectoryLauncher: ActivityResultLauncher<Uri?>,
    val googleSignInLauncher: ActivityResultLauncher<Intent>,
)

@Composable
internal fun rememberSettingsLaunchers(
    state: SettingsScreenState,
    viewModel: SettingsViewModel,
    backupViewModel: BackupSettingsViewModel,
    settings: ltd.evilcorp.atox.infrastructure.settings.Settings,
): SettingsLaunchers {
    val context = LocalContext.current

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
        val pickedUri = result.data?.let { intent ->
            androidx.core.content.IntentCompat.getParcelableExtra(
                intent,
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java
            )
        }
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

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            try {
                @Suppress("DEPRECATION")
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
                @Suppress("DEPRECATION")
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    if (state.googleSignInPurpose == GoogleSignInPurpose.Connect) {
                        settings.backupGoogleAccount = account.email.orEmpty()
                    } else {
                        backupViewModel.listGoogleDriveBackups()
                        state.showGoogleDriveRestoreDialog = true
                    }
                    return@rememberLauncherForActivityResult
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                android.util.Log.e("SettingsLaunchers", "Google Sign-In API Exception: status code ${e.statusCode}", e)
                val msg = when (e.statusCode) {
                    com.google.android.gms.common.api.CommonStatusCodes.DEVELOPER_ERROR ->
                        context.getString(ltd.evilcorp.atox.R.string.google_sign_in_developer_error)
                    else ->
                        context.getString(ltd.evilcorp.atox.R.string.google_sign_in_error_code, e.statusCode)
                }
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            } catch (e: Exception) {
                android.util.Log.e("SettingsLaunchers", "Google Sign-In unexpected error during parsing", e)
            }
        }

        if (result.resultCode != android.app.Activity.RESULT_OK) {
            android.util.Log.w("SettingsLaunchers", "Google Sign-In result not OK and no valid data: ${result.resultCode}")
            android.widget.Toast.makeText(
                context,
                context.getString(ltd.evilcorp.atox.R.string.google_sign_in_cancelled),
                android.widget.Toast.LENGTH_LONG
            ).show()
        } else {
            android.widget.Toast.makeText(
                context,
                context.getString(ltd.evilcorp.atox.R.string.google_sign_in_failed),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    return remember(
        restoreBackupLauncher,
        ringtonePickerLauncher,
        autoSaveDirectoryLauncher,
        googleSignInLauncher
    ) {
        SettingsLaunchers(
            restoreBackupLauncher = restoreBackupLauncher,
            ringtonePickerLauncher = ringtonePickerLauncher,
            autoSaveDirectoryLauncher = autoSaveDirectoryLauncher,
            googleSignInLauncher = googleSignInLauncher
        )
    }
}
