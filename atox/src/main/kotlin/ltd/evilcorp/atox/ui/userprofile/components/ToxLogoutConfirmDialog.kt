// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.userprofile.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.AtoxConfirmDialog

@Composable
fun ToxLogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AtoxConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        title = stringResource(R.string.profile_logout_confirm_title),
        text = stringResource(R.string.profile_logout_confirm),
        confirmText = stringResource(R.string.profile_logout_confirm_button),
        dismissText = stringResource(R.string.profile_logout_cancel_button),
        isDangerous = true
    )
}
