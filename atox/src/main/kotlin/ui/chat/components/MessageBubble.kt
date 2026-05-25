// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.atox.ui.stripReplyPrefix
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.Sender

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    msg: Message,
    messages: List<Message>,
    settings: Settings,
    contactName: String,
    onHaptic: () -> Unit,
    onCallHistoryClick: () -> Unit,
    fileTransfers: List<FileTransfer>,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (Message) -> Unit,
    onSaveAsClick: (Int, String) -> Unit,
    onOpenFile: (FileTransfer) -> Unit,
    onCopyMessage: (Message) -> Unit,
    onReplyMessage: (Message) -> Unit,
    onForwardMessage: (Message) -> Unit,
    onParentMessageClick: (Message) -> Unit,
    onJoinGroupClick: ((chatId: String, groupName: String) -> Unit)? = null,
    isJoinedGroup: ((chatId: String) -> Boolean)? = null
) {
    val isOutgoing = msg.sender == Sender.Sent
    val context = LocalContext.current
    val isAction = msg.type == MessageType.Action

    if (isAction) {
        val timeString = remember(msg.timestamp, settings.timeFormatPreference) {
            val time = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp
            formatChatTime(context, time, settings.timeFormatPreference)
        }
        val displayTitle = remember(msg.message) {
            when (msg.message) {
                "[CALL_HISTORY_OUTGOING]" -> context.getString(R.string.call_history_outgoing)
                "[CALL_HISTORY_INCOMING]" -> context.getString(R.string.call_history_incoming)
                "[CALL_HISTORY_MISSED]" -> context.getString(R.string.call_history_missed)
                "[CALL_HISTORY_CANCELLED]" -> context.getString(R.string.call_history_cancelled)
                
                // English compatibility
                "Outgoing call" -> context.getString(R.string.call_history_outgoing)
                "Incoming call" -> context.getString(R.string.call_history_incoming)
                "Missed call" -> context.getString(R.string.call_history_missed)
                "Cancelled call" -> context.getString(R.string.call_history_cancelled)
                
                // Russian compatibility
                "Исходящий звонок" -> context.getString(R.string.call_history_outgoing)
                "Входящий звонок" -> context.getString(R.string.call_history_incoming)
                "Пропущенный звонок" -> context.getString(R.string.call_history_missed)
                "Отменённый звонок", "Отмененный звонок" -> context.getString(R.string.call_history_cancelled)
                
                else -> msg.message
            }
        }
        CallHistoryCard(
            title = displayTitle,
            timeString = timeString,
            isOutgoing = isOutgoing,
            onClick = {
                onHaptic()
                onCallHistoryClick()
            },
        )
        return
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

    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val shape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    var showMenu by remember { mutableStateOf(false) }

    // Parse reply metadata
    val isReply = remember(msg.message) { msg.message.startsWith("[reply:") }
    val replyContent = remember(msg.message, isReply) {
        if (isReply) {
            val index = msg.message.indexOf("] ")
            if (index != -1) {
                val tag = msg.message.substring(0, index + 1)
                val parentIdentifier = tag.removePrefix("[reply:").removeSuffix("]")
                val actualText = msg.message.substring(index + 2)
                Triple(true, parentIdentifier, actualText)
            } else {
                Triple(false, "", msg.message)
            }
        } else {
            Triple(false, "", msg.message)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
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
                        showMenu = true
                    }
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Reply preview block inside the bubble
                    if (replyContent.first) {
                        val parentIdentifier = replyContent.second
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
                                            text = if (parentMsg.type == MessageType.FileTransfer) stringResource(R.string.voice_message) else stripReplyPrefix(parentMsg.message),
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

                    val isVoice = msg.type == MessageType.FileTransfer && 
                                  fileTransfers.find { it.id == msg.correlationId }?.fileName?.startsWith("voice_message_") == true

                    if (msg.type == MessageType.FileTransfer) {
                        val ft = fileTransfers.find { it.id == msg.correlationId }
                        if (ft != null) {
                            if (ft.fileName.startsWith("voice_message_")) {
                                VoiceMessageCard(
                                    ft = ft,
                                    contentColor = contentColor,
                                    onAcceptFt = onAcceptFt,
                                    onRejectFt = onRejectFt,
                                    msg = msg,
                                    isOutgoing = isOutgoing,
                                    settings = settings
                                )
                            } else {
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
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = contentColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg.message,
                                    fontSize = 15.sp,
                                    color = contentColor
                                )
                            }
                        }
                    } else {
                        val isGroupInvite = remember(msg.message) {
                            msg.message.startsWith("[GROUP_INVITE:") && msg.message.contains("|") && msg.message.endsWith("]")
                        }
                        if (isGroupInvite) {
                            GroupInviteCard(
                                msg = msg,
                                contentColor = contentColor,
                                isOutgoing = isOutgoing,
                                onHaptic = onHaptic,
                                onCancelFt = onCancelFt,
                                onJoinGroupClick = onJoinGroupClick,
                                isJoinedGroup = isJoinedGroup
                            )
                        } else {
                            val textToDisplay = if (replyContent.first) replyContent.third else msg.message
                            Text(
                                text = textToDisplay,
                                fontSize = 15.sp,
                                color = contentColor
                            )
                        }
                    }

                    if (!isVoice) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val timeString = remember(msg.timestamp, settings.timeFormatPreference) {
                            val time = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp
                            formatChatTime(context, time, settings.timeFormatPreference)
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

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (msg.type != MessageType.FileTransfer && !isAction) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy)) },
                        onClick = {
                            showMenu = false
                            onCopyMessage(msg)
                        }
                    )
                }
                if (!isAction) {
                    if (settings.enableReplies) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reply)) },
                            onClick = {
                                showMenu = false
                                onReplyMessage(msg)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.forward)) },
                        onClick = {
                            showMenu = false
                            onForwardMessage(msg)
                        }
                    )
                }
            }
        }
    }
}



@Composable
fun DateSeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            tonalElevation = 1.dp,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
