// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.atox.ui.common.ContactAvatar
import android.content.Context
import android.content.ClipboardManager
import android.widget.Toast
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.model.ReplyParser

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    msg: Message,
    messages: List<Message>,
    uiConfig: ChatUiConfig,
    contactName: String,
    onHaptic: () -> Unit,
    onCallHistoryClick: () -> Unit = {},
    fileTransfers: List<FileTransfer>,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (Message) -> Unit,
    onSaveAsClick: (Int, String) -> Unit,
    onOpenFile: (FileTransfer) -> Unit,
    onCopyMessage: ((Message) -> Unit)? = null,
    onReplyMessage: ((Message) -> Unit)? = null,
    onForwardMessage: ((Message) -> Unit)? = null,
    onParentMessageClick: ((Message) -> Unit)? = null,
    onJoinGroupClick: ((chatId: String, groupName: String) -> Unit)? = null,
    isJoinedGroup: ((chatId: String) -> Boolean)? = null,
    
    // Group chat specific parameters
    showAvatar: Boolean = false,
    senderName: String? = null,
    senderColor: Color? = null,
    avatarUri: String = ""
) {
    val isOutgoing = msg.sender == Sender.Sent
    val context = LocalContext.current

    // Polymorphic routing based on MessageType
    when (msg.type) {
        MessageType.GroupEvent -> {
            SystemEventBubble(
                msg = msg,
                uiConfig = uiConfig
            )
            return
        }
        MessageType.Action -> {
            CallHistoryBubble(
                msg = msg,
                isOutgoing = isOutgoing,
                uiConfig = uiConfig,
                onHaptic = onHaptic,
                onCallHistoryClick = onCallHistoryClick
            )
            return
        }
        else -> {
            // Normal message rendering
        }
    }

    val containerColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val shape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        if (showAvatar) {
            RoundedCornerShape(topStart = 2.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    // Parse reply metadata using domain ReplyParser
    val replyInfo = remember(msg.message) { ReplyParser.parse(msg.message) }

    val ft = remember(msg.correlationId, fileTransfers) {
        if (msg.type == MessageType.FileTransfer) {
            fileTransfers.find { it.id == msg.correlationId || it.fileNumber == msg.correlationId }
        } else {
            null
        }
    }

    val isVoice = remember(ft) {
        ft != null && ft.fileName.startsWith("voice_message_")
    }

    val isGroupInvite = remember(msg.message) {
        isGroupInviteMessage(msg.message)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Round Avatar on the left side of incoming group messages
        if (!isOutgoing && showAvatar && senderName != null) {
            ContactAvatar(
                name = senderName,
                publicKey = msg.publicKey ?: "",
                avatarUri = avatarUri,
                size = 32.dp,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            Box {
                Surface(
                    color = containerColor,
                    contentColor = contentColor,
                    shape = shape,
                    tonalElevation = 1.dp,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            onHaptic()
                            if (onCopyMessage != null || onReplyMessage != null || onForwardMessage != null) {
                                showMenu = true
                            } else {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = android.content.ClipData.newPlainText("message", msg.message)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, R.string.message_copied, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        // Colored Sender Name on top of incoming group chat messages
                        if (!isOutgoing && senderName != null && showAvatar) {
                            Text(
                                text = senderName,
                                style = MaterialTheme.typography.labelMedium,
                                color = senderColor ?: MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Polymorphic body rendering
                        when {
                            isVoice && ft != null -> {
                                VoiceMessageBubble(
                                    ft = ft,
                                    contentColor = contentColor,
                                    onAcceptFt = onAcceptFt,
                                    onRejectFt = onRejectFt,
                                    msg = msg,
                                    isOutgoing = isOutgoing,
                                    uiConfig = uiConfig
                                )
                            }
                            msg.type == MessageType.FileTransfer -> {
                                FileTransferBubble(
                                    msg = msg,
                                    ft = ft,
                                    contentColor = contentColor,
                                    onHaptic = onHaptic,
                                    onAcceptFt = onAcceptFt,
                                    onRejectFt = onRejectFt,
                                    onCancelFt = onCancelFt,
                                    onSaveAsClick = onSaveAsClick,
                                    onOpenFile = onOpenFile
                                )
                            }
                            isGroupInvite -> {
                                GroupInviteBubble(
                                    msg = msg,
                                    contentColor = contentColor,
                                    isOutgoing = isOutgoing,
                                    onHaptic = onHaptic,
                                    onCancelFt = onCancelFt,
                                    onJoinGroupClick = onJoinGroupClick,
                                    isJoinedGroup = isJoinedGroup
                                )
                            }
                            else -> {
                                TextMessageBubble(
                                    msg = msg,
                                    replyInfo = replyInfo,
                                    messages = messages,
                                    contactName = contactName,
                                    contentColor = contentColor,
                                    isOutgoing = isOutgoing,
                                    onParentMessageClick = onParentMessageClick
                                )
                            }
                        }

                        // Message time & status delivery ticks
                        if (!isVoice) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val timeString = remember(msg.timestamp, uiConfig.timeFormatPreference) {
                                val time = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp
                                formatChatTime(context, time, uiConfig.timeFormatPreference)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = timeString,
                                    fontSize = 10.sp,
                                    color = contentColor.copy(alpha = 0.7f)
                                )
                                if (isOutgoing) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (msg.timestamp == 0L) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = "Sending",
                                            tint = contentColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    } else {
                                        Box(modifier = Modifier.size(width = 18.dp, height = 12.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = "Delivered",
                                                tint = contentColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(12.dp).align(Alignment.CenterStart)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = "Delivered",
                                                tint = contentColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(12.dp).align(Alignment.CenterEnd)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Extracted Context menu dropdown
                MessageContextDropdown(
                    msg = msg,
                    uiConfig = uiConfig,
                    showMenu = showMenu,
                    onDismissRequest = { showMenu = false },
                    onCopyMessage = onCopyMessage,
                    onReplyMessage = onReplyMessage,
                    onForwardMessage = onForwardMessage,
                    isAction = false
                )
            }
        }
    }
}
