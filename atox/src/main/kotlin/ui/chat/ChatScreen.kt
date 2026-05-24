package ltd.evilcorp.atox.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.filled.KeyboardArrowDown
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.atox.ui.common.MorphingNavigationIcon
import ltd.evilcorp.atox.ui.common.PresenceTone
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.atox.ui.common.formatMessageDateHeader
import ltd.evilcorp.atox.ui.common.formatPresenceText
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusBusy
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.Sender
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Done
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Error
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.FT_NOT_STARTED
import ltd.evilcorp.core.model.FT_REJECTED
import ltd.evilcorp.core.model.isComplete
import ltd.evilcorp.core.model.isStarted
import ltd.evilcorp.core.model.isRejected
import ltd.evilcorp.core.model.MessageType
import kotlinx.coroutines.delay
import ltd.evilcorp.atox.ui.chat.components.ChatInputBar
import ltd.evilcorp.atox.ui.chat.components.MessageBubble
import ltd.evilcorp.atox.ui.chat.components.TypingBubble
import ltd.evilcorp.atox.ui.chat.components.DateSeparator

private const val CHAT_ENTER_CONTENT_DELAY_MS = 320L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contact: Contact?,
    messages: List<Message>?,
    fileTransfers: List<FileTransfer>,
    settings: Settings,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onTypingChanged: (Boolean) -> Unit,
    onSendFile: (Uri) -> Unit,
    onCallClick: () -> Unit,
    onCallHistoryClick: () -> Unit,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (Message) -> Unit,
    onSaveFt: (Int, Uri) -> Unit,
    onOpenFile: (FileTransfer) -> Unit,
    systemSoundPlayer: SystemSoundPlayer,
    replyingToMessage: Message? = null,
    onCancelReply: () -> Unit = {},
    onReplyClick: (Message) -> Unit = {},
    onCopyClick: (Message) -> Unit = {},
    onForwardClick: (Message) -> Unit = {},
    onSendVoice: (Uri) -> Unit = {},
    onJoinGroupClick: (String, String) -> Unit = { _, _ -> },
    isJoinedGroup: (String) -> Boolean = { false },
) {
    val messages = messages ?: emptyList()
    var showConversationContent by remember(contact?.publicKey) { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val showScrollToBottomFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    var lastSeenMessageId by remember { mutableStateOf<Long?>(null) }
    var unreadCount by remember { mutableStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemIndex, messages.firstOrNull()?.id) {
        if (listState.firstVisibleItemIndex <= 1) {
            unreadCount = 0
            lastSeenMessageId = messages.firstOrNull()?.id
        } else {
            val lastId = lastSeenMessageId
            if (lastId != null) {
                val index = messages.indexOfFirst { it.id == lastId }
                unreadCount = if (index > 0) index else 0
            }
        }
    }

    val performHaptic = {
        if (settings.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSendFile(uri)
        }
    }

    val activeFtIdToSave = remember { mutableStateOf(-1) }
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null && activeFtIdToSave.value != -1) {
            onSaveFt(activeFtIdToSave.value, uri)
        }
    }

    LaunchedEffect(contact?.publicKey) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    BackHandler {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(showConversationContent, messages.size, contact?.typing) {
        if (showConversationContent && messages.isNotEmpty()) {
            if (listState.firstVisibleItemIndex <= 2) {
                listState.scrollToItem(0)
            }
        }
    }

    var showCallConfirmDialog by remember { mutableStateOf(false) }

    if (showCallConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCallConfirmDialog = false },
            title = { Text(stringResource(R.string.incoming_call)) },
            text = { Text(stringResource(R.string.call_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    performHaptic()
                    showCallConfirmDialog = false
                    onCallClick()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    performHaptic()
                    showCallConfirmDialog = false
                }) {
                    Text(stringResource(R.string.reject))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {}
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = paddingValues.calculateTopPadding())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
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
                    if (showConversationContent && contact?.typing == true) {
                        item(key = "typing_bubble") {
                            TypingBubble()
                        }
                    }
                    if (!showConversationContent) {
                        return@LazyColumn
                    }
                    items(
                        count = messages.size,
                        key = { index -> messages[messages.size - 1 - index].id }
                    ) { index ->
                        val rawIndex = messages.size - 1 - index
                        val msg = messages[rawIndex]
                        val previousMsg = messages.getOrNull(rawIndex - 1)
                        val currentHeader = remember(msg.timestamp, settings.dateFormatPreference) {
                            formatMessageDateHeader(
                                context = context,
                                timestamp = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp,
                                dateFormatPreference = settings.dateFormatPreference,
                            )
                        }
                        val previousHeader = previousMsg?.let {
                            remember(it.timestamp, settings.dateFormatPreference) {
                                formatMessageDateHeader(
                                    context = context,
                                    timestamp = if (it.timestamp == 0L) System.currentTimeMillis() else it.timestamp,
                                    dateFormatPreference = settings.dateFormatPreference,
                                )
                            }
                        }

                        Column {
                            if (rawIndex == 0 || currentHeader != previousHeader) {
                                DateSeparator(label = currentHeader)
                            }

                            MessageBubble(
                                msg = msg,
                                messages = messages,
                                settings = settings,
                                contactName = contact?.name?.ifEmpty { stringResource(R.string.contact_default_name) }
                                    ?: stringResource(R.string.contact_default_name),
                                onHaptic = performHaptic,
                                onCallHistoryClick = onCallHistoryClick,
                                fileTransfers = fileTransfers,
                                onAcceptFt = onAcceptFt,
                                onRejectFt = onRejectFt,
                                onCancelFt = onCancelFt,
                                onSaveAsClick = { ftId, fileName ->
                                    activeFtIdToSave.value = ftId
                                    saveFileLauncher.launch(fileName)
                                },
                                onOpenFile = onOpenFile,
                                onCopyMessage = onCopyClick,
                                onReplyMessage = onReplyClick,
                                onForwardMessage = onForwardClick,
                                onParentMessageClick = { parentMsg ->
                                    val parentIndex = messages.indexOfFirst { it.timestamp == parentMsg.timestamp }
                                    if (parentIndex != -1) {
                                        val scrollIndex = messages.size - 1 - parentIndex
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(scrollIndex)
                                        }
                                    }
                                },
                                onJoinGroupClick = onJoinGroupClick,
                                isJoinedGroup = isJoinedGroup
                            )
                        }
                    }
                }

                if (showScrollToBottomFab) {
                    val coroutineScope = rememberCoroutineScope()
                    FloatingActionButton(
                        onClick = {
                            performHaptic()
                            coroutineScope.launch {
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
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to Bottom",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ChatInputBar(
                    contact = contact,
                    settings = settings,
                    systemSoundPlayer = systemSoundPlayer,
                    onSendMessage = onSendMessage,
                    onTypingChanged = onTypingChanged,
                    onAttachClick = { filePickerLauncher.launch("*/*") },
                    onHaptic = performHaptic,
                    replyingToMessage = replyingToMessage,
                    onCancelReply = onCancelReply,
                    onSendVoice = onSendVoice,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                )
            }
        }
    }
}
