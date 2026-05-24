package ltd.evilcorp.atox.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R

@Composable
fun RestoreBackupConfirmDialog(
    isToxStarted: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    focusManager: FocusManager
) {
    var restoreBackupPassword by remember { mutableStateOf("") }
    var restoreBackupPasswordVisible by remember { mutableStateOf(false) }

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
                OutlinedTextField(
                    value = restoreBackupPassword,
                    onValueChange = { restoreBackupPassword = it },
                    label = { Text(stringResource(R.string.backup_password_optional)) },
                    singleLine = true,
                    visualTransformation = if (restoreBackupPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { restoreBackupPasswordVisible = !restoreBackupPasswordVisible }) {
                            Icon(
                                imageVector = if (restoreBackupPasswordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (restoreBackupPasswordVisible) {
                                    stringResource(R.string.hide)
                                } else {
                                    stringResource(R.string.show)
                                },
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth(),
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
