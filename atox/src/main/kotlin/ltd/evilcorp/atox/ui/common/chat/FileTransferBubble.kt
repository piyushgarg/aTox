// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.domain.model.Message

@Composable
fun FileTransferBubble(
    msg: Message,
    ft: FileTransfer?,
    contentColor: Color,
    onHaptic: () -> Unit,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (Message) -> Unit,
    onSaveAsClick: (Int, String) -> Unit,
    onOpenFile: (FileTransfer) -> Unit,
    modifier: Modifier = Modifier
) {
    if (ft != null) {
        FileTransferCard(
            ft = ft,
            msg = msg,
            onHaptic = onHaptic,
            contentColor = contentColor,
            onAcceptFt = onAcceptFt,
            onRejectFt = onRejectFt,
            onCancelFt = onCancelFt,
            onSaveAsClick = onSaveAsClick,
            onOpenFile = onOpenFile
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = msg.message,
                fontSize = 15.sp,
                color = contentColor
            )
        }
    }
}
