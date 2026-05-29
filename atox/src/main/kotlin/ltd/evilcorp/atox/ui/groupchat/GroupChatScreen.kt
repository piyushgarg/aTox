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
import androidx.compose.ui.graphics.toArgb
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
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.common.AtoxConfirmDialog
import ltd.evilcorp.atox.ui.groupchat.components.GroupChatAppBar
import ltd.evilcorp.atox.ui.groupchat.components.GroupPeersSheet
import ltd.evilcorp.atox.ui.groupchat.components.GroupInviteSheet
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import ltd.evilcorp.atox.ui.theme.getPeerColor
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyListState

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
    voiceRecorder: ltd.evilcorp.domain.features.call.service.IVoiceRecorder,
    isExpanded: Boolean = false,
) {
    val group = groupState.value
    val messages = messagesState.value ?: emptyList()
    val peers = peersState.value ?: emptyList()
    val connStatus = connectionStatusState.value
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val selfAvatarUri = selfAvatarUriState.value

    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val transitionAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0.85f else 0.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "topBarAlpha"
    )
    val transitionElevation by animateDpAsState(
        targetValue = if (isScrolled) 4.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "topBarElevation"
    )

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

    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) {
                return@remember ctx
            }
            ctx = ctx.baseContext
        }
        null
    }
    val navBarColor = MaterialTheme.colorScheme.surfaceContainer
    val originalNavBarColor = remember(activity) { activity?.window?.navigationBarColor ?: 0 }
    androidx.compose.runtime.DisposableEffect(navBarColor) {
        activity?.window?.let { window ->
            window.navigationBarColor = navBarColor.toArgb()
        }
        onDispose {
            activity?.window?.let { window ->
                window.navigationBarColor = originalNavBarColor
            }
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

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = transitionElevation,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = transitionAlpha),
                modifier = Modifier.fillMaxWidth()
            ) {
                GroupChatAppBar(
                    group = group,
                    peers = peers,
                    connStatus = connStatus,
                    uiConfig = uiConfig,
                    onBack = onBack,
                    onInviteClick = { showInviteDialog = true },
                    onPeersClick = { showPeersDialog = true },
                    onLeaveClick = { showLeaveDialog = true },
                    containerColor = Color.Transparent,
                    isExpanded = isExpanded
                )
            }
        }
    ) { paddingValues ->
        val isOffline = connStatus != GroupConnectionStatus.Connected
        val isReconnecting = connStatus == GroupConnectionStatus.Reconnecting

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (isOffline) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isReconnecting) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isReconnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.group_reconnecting),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.group_offline_banner),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
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
                    val peerColor = getPeerColor(msg.colorIndex)
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
                voiceRecorder = voiceRecorder,
                contact = null,
                onCopyClick = {},
                onReplyClick = {},
                onForwardClick = {},
                listState = listState
            )
        }
    }
}
}

