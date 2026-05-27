// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.backup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ltd.evilcorp.atox.R

@Composable
fun GoogleAccountDialog(
    googleAccountInput: String,
    onGoogleAccountInputChange: (String) -> Unit,
    onChooseAccountClick: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_google_account)) },
        text = {
            OutlinedTextField(
                value = googleAccountInput,
                onValueChange = onGoogleAccountInputChange,
                label = { Text(stringResource(R.string.backup_google_account)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onChooseAccountClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.backup_google_account_not_selected)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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
