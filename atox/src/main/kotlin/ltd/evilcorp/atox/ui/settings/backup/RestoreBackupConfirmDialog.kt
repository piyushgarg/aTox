package ltd.evilcorp.atox.ui.settings.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.AtoxPasswordField

@Composable
fun RestoreBackupConfirmDialog(
    isToxStarted: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    focusManager: FocusManager
) {
    var restoreBackupPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    if (isToxStarted) {
                        stringResource(R.string.backup_restore_confirm_logged_in)
                    } else {
                        stringResource(R.string.backup_restore_confirm)
                    }
                )
                AtoxPasswordField(
                    value = restoreBackupPassword,
                    onValueChange = { restoreBackupPassword = it },
                    label = stringResource(R.string.backup_password_optional),
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(restoreBackupPassword)
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reject))
            }
        }
    )
}
