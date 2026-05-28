// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.AtoxConfirmDialog

@Composable
fun CallConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AtoxConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        title = stringResource(R.string.incoming_call),
        text = stringResource(R.string.call_confirm),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.reject)
    )
}
