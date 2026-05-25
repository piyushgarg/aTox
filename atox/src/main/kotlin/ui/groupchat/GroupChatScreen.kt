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
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.atox.ui.chat.components.ChatInputBar
import android.widget.Toast
import ltd.evilcorp.atox.ui.chat.components.VoiceMessageCard
import ltd.evilcorp.atox.ui.chat.components.FileTransferCard
import ltd.evilcorp.atox.ui.groupchat.components.GroupPeersSheet
import ltd.evilcorp.atox.ui.groupchat.components.GroupInviteSheet
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.GroupPeer
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.model.FileTransfer
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

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {},
        bottomBar = {}
    ) { paddingValues ->
        val showScrollToBottomFab by remember {
            derivedStateOf { listState.firstVisibleItemIndex > 2 }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = paddingValues.calculateTopPadding())
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
                                ltd.evilcorp.atox.ui.chat.components.DateSeparator(label = currentHeader)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            GroupMessageBubble(
                                msg = msg,
                                settings = settings,
                                peers = peers,
                                contacts = contacts,
                                fileTransfers = fileTransfers,
                                onAcceptFt = { ftId ->
                                    performHaptic()
                                    onAcceptFt(ftId)
                                },
                                onRejectFt = { ftId ->
                                    performHaptic()
                                    onRejectFt(ftId)
                                },
                                onCancelFt = { m ->
                                    performHaptic()
                                    onCancelFt(m)
                                },
                                onSaveAsClick = { ftId, fileName ->
                                    performHaptic()
                                    activeFtIdToSave.value = ftId
                                    saveFileLauncher.launch(fileName)
                                },
                                onOpenFile = onOpenFile,
                                onHaptic = performHaptic
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupMessageBubble(
    msg: GroupMessage,
    settings: Settings,
    peers: List<GroupPeer>,
    contacts: List<Contact>,
    fileTransfers: List<FileTransfer>,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (GroupMessage) -> Unit,
    onSaveAsClick: (Int, String) -> Unit,
    onOpenFile: (FileTransfer) -> Unit,
    onHaptic: () -> Unit
) {
    val context = LocalContext.current

    if (msg.type == MessageType.GroupEvent) {
        val timeString = remember(msg.timestamp, settings.timeFormatPreference) {
            val time = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp
            formatChatTime(context, time, settings.timeFormatPreference)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                tonalElevation = 1.dp,
            ) {
                Text(
                    text = msg.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 10.sp,
            )
        }
        return
    }

    val isOutgoing = msg.sender == Sender.Sent
    val isAction = msg.type == MessageType.Action

    val senderPeer = peers.find { it.peerId == msg.peerId }
    val senderName = senderPeer?.name ?: msg.senderName

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
        RoundedCornerShape(topStart = 2.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    // Map GroupMessage to regular Message object to perfectly satisfy shared FileTransfer Card interfaces!
    val mappedMessage = remember(msg) {
        ltd.evilcorp.core.model.Message(
            publicKey = msg.groupChatId,
            message = msg.message,
            sender = msg.sender,
            type = msg.type,
            correlationId = msg.correlationId,
            timestamp = msg.timestamp
        ).apply { this.id = msg.id }
    }

    val peerColor = remember(senderName) { getPeerColor(senderName) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // 1. Telegram Style Round Avatar on the left side of incoming messages!
        if (!isOutgoing) {
            val matchingContact = contacts.find { it.publicKey.equals(senderPeer?.publicKey ?: "", ignoreCase = true) }
            ContactAvatar(
                name = senderName,
                publicKey = senderPeer?.publicKey ?: "",
                avatarUri = matchingContact?.avatarUri ?: "",
                size = 32.dp,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 2. Message Bubble Column
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = containerColor,
                contentColor = contentColor,
                shape = shape,
                tonalElevation = 1.dp,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                        onHaptic()
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("message", msg.message)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, R.string.message_copied, android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // 3. Colored Sender Name on top of incoming group chat messages!
                    if (!isOutgoing) {
                        Text(
                            text = senderName,
                            style = MaterialTheme.typography.labelMedium,
                            color = peerColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val isVoice = if (msg.type == MessageType.FileTransfer) {
                        val ft = fileTransfers.find { it.fileNumber == msg.correlationId || it.id == msg.correlationId }
                        ft != null && ft.fileName.startsWith("voice_message_")
                    } else {
                        false
                    }

                    // 4. Message Content (Text / File / Voice card)
                    if (msg.type == MessageType.FileTransfer) {
                        val ft = fileTransfers.find { it.fileNumber == msg.correlationId || it.id == msg.correlationId }
                        if (ft != null) {
                            if (ft.fileName.startsWith("voice_message_")) {
                                VoiceMessageCard(
                                    ft = ft,
                                    contentColor = contentColor,
                                    onAcceptFt = onAcceptFt,
                                    onRejectFt = onRejectFt,
                                    msg = mappedMessage,
                                    isOutgoing = isOutgoing,
                                    settings = settings
                                )
                            } else {
                                FileTransferCard(
                                    ft = ft,
                                    msg = mappedMessage,
                                    onHaptic = onHaptic,
                                    contentColor = contentColor,
                                    onAcceptFt = onAcceptFt,
                                    onRejectFt = onRejectFt,
                                    onCancelFt = { onCancelFt(msg) },
                                    onSaveAsClick = onSaveAsClick,
                                    onOpenFile = onOpenFile
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = contentColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg.message,
                                    fontSize = 15.sp,
                                    color = contentColor
                                )
                            }
                        }
                    } else {
                        // Render standard Text or Action messages
                        val isImageMarker = msg.message.startsWith("[FILE:") && msg.message.contains("|")
                        val isVoiceMarker = msg.message.startsWith("[VOICE:") && msg.message.contains("|")
                        
                        val displayText = when {
                            isImageMarker -> stringResource(R.string.ft_status_waiting)
                            isVoiceMarker -> stringResource(R.string.voice_message)
                            else -> msg.message
                        }
                        
                        Text(
                            text = displayText,
                            fontSize = 15.sp,
                            color = contentColor
                        )
                    }

                    // 5. Chat Time and Sending Indicators
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
        }
    }
}
