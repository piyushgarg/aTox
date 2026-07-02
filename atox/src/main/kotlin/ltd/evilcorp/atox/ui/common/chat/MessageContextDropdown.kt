// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType

@Composable
fun MessageContextDropdown(
    msg: Message,
    uiConfig: ChatUiConfig,
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    onCopyMessage: ((Message) -> Unit)?,
    onReplyMessage: ((Message) -> Unit)?,
    onForwardMessage: ((Message) -> Unit)?,
    isAction: Boolean
) {
    val hasMenuItems = onCopyMessage != null || onReplyMessage != null || onForwardMessage != null

    if (!hasMenuItems) return

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismissRequest
    ) {
        if (msg.type != MessageType.FileTransfer && !isAction && onCopyMessage != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy)) },
                onClick = {
                    onDismissRequest()
                    onCopyMessage(msg)
                }
            )
        }
        if (!isAction) {
            if (uiConfig.enableReplies && onReplyMessage != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reply)) },
                    onClick = {
                        onDismissRequest()
                        onReplyMessage(msg)
                    }
                )
            }
            if (onForwardMessage != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.forward)) },
                    onClick = {
                        onDismissRequest()
                        onForwardMessage(msg)
                    }
                )
            }
        }
    }
}
