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
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.atox.ui.common.chat.ChatInputBar
import ltd.evilcorp.atox.ui.common.chat.MessageBubble
import ltd.evilcorp.atox.ui.common.chat.DateSeparator
import android.widget.Toast
import ltd.evilcorp.atox.ui.navigation.AppBarStateHolder
import ltd.evilcorp.atox.ui.navigation.AppBarConfig
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.groupchat.components.GroupPeersSheet
import ltd.evilcorp.atox.ui.groupchat.components.GroupInviteSheet
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.DateFormatPreference
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.domain.model.Group
import ltd.evilcorp.domain.model.GroupMessage
import ltd.evilcorp.domain.model.GroupPeer
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.model.FileTransfer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Material 3 Pastel Colors for Sender Names and Avatars
private val PeerColors = listOf(
    Color(0xFFBA68C8), // Purple
    Color(0xFF7986CB), // Indigo
    Color(0xFF64B5F6), // Blue
    Color(0xFF4DD0E1), // Cyan
    Color(0xFF4DB6AC), // Teal
    Color(0xFF81C784), // Green
    Color(0xFFFFB74D), // Orange
    Color(0xFFF06292), // Pink
)

private fun getPeerColor(peerName: String): Color {
    val hash = peerName.hashCode().let { if (it < 0) -it else it }
    return PeerColors[hash % PeerColors.size]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupState: State<Group?>,
    messagesState: State<List<GroupMessage>?>,
    peersState: State<List<GroupPeer>?>,
    contactsState: State<List<Contact>>,
    connectionStatusState: State<GroupConnectionStatus>,
    fileTransfersState: State<List<FileTransfer>>,
    settings: Settings,
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
    val mappedMessages = remember(messages) {
        messages.map { m ->
            ltd.evilcorp.domain.model.Message(
                publicKey = m.groupChatId,
                message = m.message,
                sender = m.sender,
                type = m.type,
                correlationId = m.correlationId,
                timestamp = m.timestamp
            ).apply { this.id = m.id }
        }
    }
    val peers = peersState.value ?: emptyList()
    val connStatus = connectionStatusState.value
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (listState.firstVisibleItemIndex <= 2) {
                listState.scrollToItem(0)
            }
        }
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val selfAvatarUri = remember {
        val file = java.io.File(context.filesDir, "self_avatar.png")
        if (file.exists()) file.toURI().toString() else ""
    }

    val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
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

    val inviteCopiedText = stringResource(R.string.group_invite_copied)
    val inviteSentText = stringResource(R.string.group_invite_sent)
    val inviteFailedText = stringResource(R.string.group_invite_failed)

    val scope = rememberCoroutineScope()
    var isCopying by remember { mutableStateOf(false) }
    var invitingContactId by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onSendFile(uri)
        }
    }

    val activeFtIdToSave = remember { mutableStateOf(-1) }
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: android.net.Uri? ->
        if (uri != null && activeFtIdToSave.value != -1) {
            onSaveAsClick(activeFtIdToSave.value, uri.toString())
        }
    }

    val performHaptic = {
        if (settings.hapticEnabled) {
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
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text(stringResource(R.string.group_leave)) },
            text = { Text(stringResource(R.string.group_leave_confirm, group?.name ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onLeaveGroup()
                    onBack()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
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
            settings = settings,
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

    LaunchedEffect(group, group?.name, connStatus, peers.size, menuExpanded, showInviteDialog, showPeersDialog, showLeaveDialog, settings.hapticEnabled) {
        AppBarStateHolder.register(
            route = AppRoutes.GroupChat::class.qualifiedName!!,
            cfg = AppBarConfig(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (settings.hapticEnabled) {
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
                                if (settings.hapticEnabled) {
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
                            if (settings.hapticEnabled) {
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
                                    if (settings.hapticEnabled) {
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
                                    if (settings.hapticEnabled) {
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
                                    if (settings.hapticEnabled) {
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
    }

    DisposableEffect(Unit) {
        onDispose {
            AppBarStateHolder.unregister(AppRoutes.GroupChat::class.qualifiedName!!)
        }
    }

    val showScrollToBottomFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp)
                ) {
                    items(
                        count = messages.size,
                        key = { index -> messages[messages.size - 1 - index].id }
                    ) { index ->
                        val rawIndex = messages.size - 1 - index
                        val msg = messages[rawIndex]
                        val previousMsg = messages.getOrNull(rawIndex - 1)
                        val currentHeader = remember(msg.timestamp, settings.dateFormatPreference) {
                            ltd.evilcorp.atox.ui.common.formatMessageDateHeader(
                                context = context,
                                timestamp = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp,
                                dateFormatPreference = settings.dateFormatPreference,
                            )
                        }
                        val previousHeader = previousMsg?.let {
                            remember(it.timestamp, settings.dateFormatPreference) {
                                ltd.evilcorp.atox.ui.common.formatMessageDateHeader(
                                    context = context,
                                    timestamp = if (it.timestamp == 0L) System.currentTimeMillis() else it.timestamp,
                                    dateFormatPreference = settings.dateFormatPreference,
                                )
                            }
                        }

                        Column {
                            if (rawIndex == 0 || currentHeader != previousHeader) {
                                DateSeparator(label = currentHeader)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            val isOutgoing = msg.sender == Sender.Sent
                            val senderPeer = peers.find { it.peerId == msg.peerId }
                            val senderName = senderPeer?.name ?: msg.senderName
                            val peerColor = remember(senderName) { getPeerColor(senderName) }
                            val matchingContact = contacts.find { it.publicKey.equals(senderPeer?.publicKey ?: "", ignoreCase = true) }
                            val mappedMessage = mappedMessages[rawIndex]

                            MessageBubble(
                                msg = mappedMessage,
                                messages = mappedMessages,
                                settings = settings,
                                contactName = senderName,
                                onHaptic = performHaptic,
                                fileTransfers = fileTransfers,
                                onAcceptFt = onAcceptFt,
                                onRejectFt = onRejectFt,
                                onCancelFt = { onCancelFt(msg) },
                                onSaveAsClick = { ftId, fileName ->
                                    performHaptic()
                                    activeFtIdToSave.value = ftId
                                    saveFileLauncher.launch(fileName)
                                },
                                onOpenFile = onOpenFile,
                                showAvatar = !isOutgoing,
                                senderName = if (isOutgoing) null else senderName,
                                senderColor = if (isOutgoing) null else peerColor,
                                avatarUri = if (isOutgoing) "" else (matchingContact?.avatarUri ?: "")
                            )
                        }
                    }
                }

                if (showScrollToBottomFab) {
                    FloatingActionButton(
                        onClick = {
                            performHaptic()
                            scope.launch {
                                listState.scrollToItem(0)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to Bottom",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Beautiful Unified Chat Input Bar!
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ChatInputBar(
                    contact = null, // Passing null forces group chat defaults
                    settings = settings,
                    systemSoundPlayer = systemSoundPlayer,
                    onSendMessage = { msg ->
                        performHaptic()
                        onSendMessage(msg)
                    },
                    onTypingChanged = {},
                    onAttachClick = {
                        performHaptic()
                        filePickerLauncher.launch("*/*")
                    },
                    onHaptic = performHaptic,
                    replyingToMessage = null,
                    onCancelReply = {},
                    onSendVoice = { uri ->
                        performHaptic()
                        onSendVoice(uri)
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                )
            }
        }
}

