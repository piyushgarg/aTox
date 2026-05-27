// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import ltd.evilcorp.atox.R

@Suppress("FunctionNaming")
@Composable
fun AtoxConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String,
    confirmText: String,
    dismissText: String = stringResource(R.string.reject),
    isDangerous: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val triggerHaptic = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = icon,
        onDismissRequest = {
            triggerHaptic()
            onDismiss()
        },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = {
                    triggerHaptic()
                    onConfirm()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDangerous) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    triggerHaptic()
                    onDismiss()
                }
            ) {
                Text(dismissText)
            }
        }
    )
}
