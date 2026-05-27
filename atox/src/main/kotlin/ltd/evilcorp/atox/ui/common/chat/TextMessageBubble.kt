// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.stripReplyPrefix
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.ReplyInfo
import ltd.evilcorp.domain.model.Sender

@Composable
fun TextMessageBubble(
    msg: Message,
    replyInfo: ReplyInfo,
    messages: List<Message>,
    contactName: String,
    contentColor: Color,
    isOutgoing: Boolean,
    onParentMessageClick: ((Message) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Reply preview block inside the bubble
        if (replyInfo.isReply && onParentMessageClick != null) {
            val parentIdentifier = replyInfo.parentIdentifier
            val parentMsg = remember(messages, parentIdentifier) {
                messages.find { 
                    it.message.hashCode().toString() == parentIdentifier || 
                    it.timestamp.toString() == parentIdentifier
                }
            }
            if (parentMsg != null) {
                val replyTitleColor = if (isOutgoing) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.primary
                }
                val replyBarColor = if (isOutgoing) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Surface(
                    color = contentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clickable { onParentMessageClick(parentMsg) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(replyBarColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
                            Text(
                                text = if (parentMsg.sender == Sender.Sent) stringResource(R.string.reply_you) else contactName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = replyTitleColor
                            )
                            Text(
                                text = if (parentMsg.type == MessageType.FileTransfer) {
                                    stringResource(R.string.voice_message)
                                } else {
                                    stripReplyPrefix(parentMsg.message)
                                },
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = contentColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        val textToDisplay = if (replyInfo.isReply) replyInfo.actualText else msg.message
        Text(
            text = textToDisplay,
            fontSize = 15.sp,
            color = contentColor
        )
    }
}
