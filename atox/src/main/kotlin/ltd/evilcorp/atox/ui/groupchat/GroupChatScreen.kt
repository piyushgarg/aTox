package ltd.evilcorp.atox.ui.groupchat

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.atox.ui.common.chat.ChatScreenContent
import ltd.evilcorp.atox.ui.common.chat.MessageBubbleConfig
import android.widget.Toast
import ltd.evilcorp.atox.ui.navigation.AppBarStateHolder
import ltd.evilcorp.atox.ui.navigation.AppBarConfig
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.common.AtoxAppBar
import ltd.evilcorp.atox.ui.common.AtoxConfirmDialog
import ltd.evilcorp.atox.ui.groupchat.components.GroupPeersSheet
import ltd.evilcorp.atox.ui.groupchat.components.GroupInviteSheet
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.DateFormatPreference
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.domain.model.Group
import ltd.evilcorp.domain.model.GroupPeer
import ltd.evilcorp.domain.model.GroupMessage
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.model.FileTransfer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import ltd.evilcorp.atox.ui.theme.getPeerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupState: State<Group?>,
    messagesState: State<List<GroupMessage>?>,
    peersState: State<List<GroupPeer>?>,
    contactsState: State<List<Contact>>,
    connectionStatusState: State<GroupConnectionStatus>,
    fileTransfersState: State<List<FileTransfer>>,
    selfAvatarUriState: State<String>,
    uiConfig: ChatUiConfig,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendFile: (android.net.Uri) -> Unit,
    onSendVoice: (android.net.Uri) -> Unit,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (GroupMessage) -> Unit,
    onSaveAsClick: (Int, String) -> Unit,
    onOpenFile: (FileTransfer) -> Unit,
    onLeaveGroup: () -> Unit,
    onCopyInvite: () -> Unit,
    onInviteFriend: (friendPublicKey: String) -> Unit,
    systemSoundPlayer: SystemSoundPlayer,
    onInviteClick: (() -> Unit) -> Unit = {},
    onPeersClick: (() -> Unit) -> Unit = {},
    onLeaveClick: (() -> Unit) -> Unit = {},
    onGroupInfoChanged: (name: String, topic: String, peersCount: Int, status: GroupConnectionStatus) -> Unit = { _, _, _, _ -> },
) {
    val group = groupState.value
    val messages = messagesState.value ?: emptyList()
    val peers = peersState.value ?: emptyList()
    val connStatus = connectionStatusState.value
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val selfAvatarUri = selfAvatarUriState.value

    var showLeaveDialog by remember { mutableStateOf(false) }
    var showPeersDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(showInviteDialog) {
        if (!showInviteDialog) {
            inviteSearchQuery = ""
        }
    }

    var inviteResultText by remember { mutableStateOf<String?>(null) }
    val contacts = contactsState.value
    val fileTransfers = fileTransfersState.value

    val performHaptic = {
        if (uiConfig.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(group, connStatus) {
        onGroupInfoChanged(
            group?.name.orEmpty(),
            group?.topic.orEmpty(),
            group?.peerCount ?: 0,
            connStatus
        )
    }

    DisposableEffect(Unit) {
        onInviteClick { showInviteDialog = true }
        onPeersClick { showPeersDialog = true }
        onLeaveClick { showLeaveDialog = true }
        onDispose {
            onInviteClick {}
            onPeersClick {}
            onLeaveClick {}
        }
    }

    LaunchedEffect(group?.chatId) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }


    if (showLeaveDialog) {
        AtoxConfirmDialog(
            onDismiss = { showLeaveDialog = false },
            onConfirm = {
                showLeaveDialog = false
                onLeaveGroup()
                onBack()
            },
            title = stringResource(R.string.group_leave),
            text = stringResource(R.string.group_leave_confirm, group?.name ?: ""),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(android.R.string.cancel),
            isDangerous = true
        )
    }

    if (showPeersDialog) {
        GroupPeersSheet(
            onDismissRequest = { showPeersDialog = false },
            peers = peers,
            contacts = contacts,
            selfAvatarUri = selfAvatarUri
        )
    }

    if (showInviteDialog) {
        GroupInviteSheet(
            onDismissRequest = { showInviteDialog = false },
            onCopyInvite = onCopyInvite,
            onInviteFriend = onInviteFriend,
            peers = peers,
            contacts = contacts,
            onInviteResult = { inviteResultText = it }
        )
    }

    if (inviteResultText != null) {
        AlertDialog(
            onDismissRequest = { inviteResultText = null },
            text = { Text(inviteResultText!!) },
            confirmButton = {
                TextButton(onClick = { inviteResultText = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    var menuExpanded by remember { mutableStateOf(false) }
    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer

    AtoxAppBar(
        route = AppRoutes.GroupChat::class.qualifiedName!!,
        config = AppBarConfig(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (uiConfig.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                val id = group?.chatId ?: ""
                                if (id.isNotEmpty()) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("group ID", id)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.group_invite_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = group?.name?.ifEmpty {
                                context.getString(R.string.contact_default_name)
                            } ?: context.getString(R.string.contact_default_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = when (connStatus) {
                                GroupConnectionStatus.Connected -> ltd.evilcorp.atox.ui.theme.StatusAvailable
                                GroupConnectionStatus.Connecting,
                                GroupConnectionStatus.Reconnecting -> ltd.evilcorp.atox.ui.theme.StatusAway
                                GroupConnectionStatus.Disconnected -> ltd.evilcorp.atox.ui.theme.StatusOffline
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val statusText = when (connStatus) {
                                GroupConnectionStatus.Connected -> context.getString(R.string.group_connected)
                                GroupConnectionStatus.Connecting,
                                GroupConnectionStatus.Reconnecting -> context.getString(R.string.group_connecting)
                                GroupConnectionStatus.Disconnected -> context.getString(R.string.group_offline)
                            }
                            Text(
                                text = "$statusText • ${context.getString(R.string.group_peer_count, peers.size)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                Box(modifier = Modifier.padding(start = 4.dp)) {
                    ltd.evilcorp.atox.ui.common.MorphingNavigationIcon(
                        isBack = true,
                        onClick = {
                            if (uiConfig.hapticEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onBack()
                        }
                    )
                }
            },
            actions = {
                Box {
                    IconButton(onClick = {
                        if (uiConfig.hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        menuExpanded = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.group_invite_friend)) },
                            leadingIcon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                if (uiConfig.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                showInviteDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.group_peers)) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                if (uiConfig.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                showPeersDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.group_leave), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                menuExpanded = false
                                if (uiConfig.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                showLeaveDialog = true
                            }
                        )
                    }
                }
            },
            containerColor = surfaceContainerColor
        )
    )

    val peerColorsCache = remember { mutableMapOf<String, Color>() }

    ChatScreenContent(
        messages = messages,
        toMessage = { m ->
            Message(
                publicKey = m.groupChatId,
                message = m.message,
                sender = m.sender,
                type = m.type,
                correlationId = m.correlationId,
                timestamp = m.timestamp
            ).apply { this.id = m.id }
        },
        getBubbleConfig = { msg ->
            val isOutgoing = msg.sender == Sender.Sent
            val senderPeer = peers.find { it.peerId == msg.peerId }
            val senderName = senderPeer?.name ?: msg.senderName
            val peerColor = peerColorsCache.getOrPut(senderName) { getPeerColor(senderName) }
            val matchingContact = contacts.find { it.publicKey.equals(senderPeer?.publicKey ?: "", ignoreCase = true) }
            MessageBubbleConfig(
                contactName = senderName,
                showAvatar = !isOutgoing,
                senderName = if (isOutgoing) null else senderName,
                senderColor = if (isOutgoing) null else peerColor,
                avatarUri = if (isOutgoing) "" else (matchingContact?.avatarUri ?: "")
            )
        },
        uiConfig = uiConfig,
        fileTransfers = fileTransfers,
        onSendMessage = { msg ->
            performHaptic()
            onSendMessage(msg)
        },
        onTypingChanged = {},
        onSendFile = onSendFile,
        onSendVoice = { uri ->
            performHaptic()
            onSendVoice(uri)
        },
        onAcceptFt = onAcceptFt,
        onRejectFt = onRejectFt,
        onCancelFt = onCancelFt,
        onSaveFt = { ftId, uri ->
            onSaveAsClick(ftId, uri.toString())
        },
        onOpenFile = onOpenFile,
        systemSoundPlayer = systemSoundPlayer,
        performHaptic = performHaptic,
        contact = null,
        onCopyClick = {},
        onReplyClick = {},
        onForwardClick = {}
    )
}

