// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.chat.model.Message

@Composable
fun GroupInviteCard(
    msg: Message,
    contentColor: Color,
    isOutgoing: Boolean,
    onHaptic: () -> Unit,
    onCancelFt: (Message) -> Unit,
    onJoinGroupClick: ((chatId: String, groupName: String) -> Unit)?,
    isJoinedGroup: ((chatId: String) -> Boolean)?
) {
    val payload = remember(msg.message) {
        msg.message.removePrefix("[GROUP_INVITE:").removeSuffix("]")
    }
    val parts = remember(payload) {
        payload.split("|")
    }
    val groupName = parts.getOrNull(0).orEmpty()
    val chatIdOrBytes = parts.getOrNull(1).orEmpty()

    Column(
        modifier = Modifier
            .width(240.dp)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = contentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = groupName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.group_invite),
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
        
        Text(
            text = "Вас пригласили присоединиться к групповому чату.",
            fontSize = 13.sp,
            color = contentColor.copy(alpha = 0.8f)
        )

        if (!isOutgoing) {
            val joined = remember(chatIdOrBytes, isJoinedGroup) {
                isJoinedGroup?.invoke(chatIdOrBytes) ?: false
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onHaptic()
                        onCancelFt(msg)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = contentColor.copy(alpha = 0.6f))
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onHaptic()
                        onJoinGroupClick?.invoke(chatIdOrBytes, groupName)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (joined) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (joined) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (joined) "Перейти" else "Вступить",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
