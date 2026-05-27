// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ltd.evilcorp.atox.ui.chat.components.GroupInviteCard
import ltd.evilcorp.domain.model.Message

@Composable
fun GroupInviteBubble(
    msg: Message,
    contentColor: Color,
    isOutgoing: Boolean,
    onHaptic: () -> Unit,
    onCancelFt: (Message) -> Unit,
    onJoinGroupClick: ((chatId: String, groupName: String) -> Unit)?,
    isJoinedGroup: ((chatId: String) -> Boolean)?
) {
    GroupInviteCard(
        msg = msg,
        contentColor = contentColor,
        isOutgoing = isOutgoing,
        onHaptic = onHaptic,
        onCancelFt = onCancelFt,
        onJoinGroupClick = onJoinGroupClick,
        isJoinedGroup = isJoinedGroup
    )
}

fun isGroupInviteMessage(messageText: String): Boolean {
    return messageText.startsWith("[GROUP_INVITE:") && messageText.contains("|") && messageText.endsWith("]")
}
