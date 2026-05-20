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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.common.ContactAvatar
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

private const val CHAT_ENTER_CONTENT_DELAY_MS = 220L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactState: State<Contact?>,
    messagesState: State<List<Message>?>,
    fileTransfersState: State<List<FileTransfer>>,
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
) {
    val contact = contactState.value
    val messages = messagesState.value ?: emptyList()
    var showConversationContent by remember(contact?.publicKey) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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

    var textInput by remember { mutableStateOf("") }
    LaunchedEffect(contact?.publicKey) {
        showConversationContent = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        delay(CHAT_ENTER_CONTENT_DELAY_MS)
        showConversationContent = true
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
                listState.animateScrollToItem(0)
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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val name = contact?.name?.ifEmpty { stringResource(R.string.contact_default_name) } 
                            ?: stringResource(R.string.contact_default_name)
                        val presence = contact?.let {
                            formatPresenceText(
                                context = context,
                                contact = it,
                                dateFormatPreference = settings.dateFormatPreference,
                                timeFormatPreference = settings.timeFormatPreference,
                            )
                        }

                        ContactAvatar(
                            name = name,
                            publicKey = contact?.publicKey.orEmpty(),
                            avatarUri = contact?.avatarUri.orEmpty(),
                            size = 36.dp,
                            fontSize = 14.sp,
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.wrapContentHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = presence?.text ?: stringResource(R.string.chat_status_offline),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = when (presence?.color) {
                                    PresenceTone.Online -> StatusAvailable
                                    PresenceTone.Away -> StatusAway
                                    PresenceTone.Busy -> StatusBusy
                                    PresenceTone.Accent -> StatusAvailable // Standout vibrant green for writing status!
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        performHaptic()
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigation_drawer_close), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    val isOnline = contact?.connectionStatus != ltd.evilcorp.core.model.ConnectionStatus.None
                    IconButton(onClick = {
                        performHaptic()
                        if (isOnline) {
                            if (settings.confirmCalling) {
                                showCallConfirmDialog = true
                            } else {
                                onCallClick()
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.cannot_call_offline), Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { performHaptic() }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
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
                                settings = settings,
                                onHaptic = performHaptic,
                                onCallHistoryClick = onCallHistoryClick,
                                fileTransfers = fileTransfersState.value,
                                onAcceptFt = onAcceptFt,
                                onRejectFt = onRejectFt,
                                onCancelFt = onCancelFt,
                                onSaveAsClick = { ftId, fileName ->
                                    activeFtIdToSave.value = ftId
                                    saveFileLauncher.launch(fileName)
                                },
                                onOpenFile = onOpenFile
                            )
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it
                            onTypingChanged(it.isNotBlank())
                        },
                        placeholder = { Text(stringResource(R.string.chat_write_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true,
                            keyboardType = KeyboardType.Text,
                        ),
                        maxLines = 4,
                        trailingIcon = {
                            val isOnline = contact?.connectionStatus != ltd.evilcorp.core.model.ConnectionStatus.None
                            IconButton(onClick = {
                                performHaptic()
                                if (isOnline) {
                                    filePickerLauncher.launch("*/*")
                                } else {
                                    Toast.makeText(context, context.getString(R.string.cannot_send_file_offline), Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Attachment,
                                    contentDescription = "Attach File",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp).graphicsLayer(rotationZ = -45f)
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (textInput.trim().isNotEmpty()) {
                                performHaptic()
                                onSendMessage(textInput)
                                systemSoundPlayer.playSentSound(settings.sentMessageSoundUri, settings.sentMessageSoundVolume)
                                onTypingChanged(false)
                                textInput = ""
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
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

@Composable
fun MessageBubble(
    msg: Message,
    settings: Settings,
    onHaptic: () -> Unit,
    onCallHistoryClick: () -> Unit,
    fileTransfers: List<FileTransfer>,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (Message) -> Unit,
    onSaveAsClick: (Int, String) -> Unit,
    onOpenFile: (FileTransfer) -> Unit
) {
    val isOutgoing = msg.sender == Sender.Sent
    val context = LocalContext.current
    val isAction = msg.type == MessageType.Action

    if (isAction) {
        val timeString = remember(msg.timestamp, settings.timeFormatPreference) {
            val time = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp
            formatChatTime(context, time, settings.timeFormatPreference)
        }
        CallHistoryCard(
            title = msg.message,
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (msg.type == MessageType.FileTransfer) {
                    val ft = fileTransfers.find { it.id == msg.correlationId }
                    if (ft != null) {
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
                    Text(
                        text = msg.message,
                        fontSize = 15.sp,
                        color = contentColor // EXPLICITLY set text color to guarantee readability
                    )
                }
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

@Composable
private fun CallHistoryCard(
    title: String,
    timeString: String,
    isOutgoing: Boolean,
    onClick: () -> Unit,
) {
    val missed = title.contains("missed", ignoreCase = true) || title.contains("пропущ", ignoreCase = true)
    val cancelled = title.contains("cancel", ignoreCase = true) || title.contains("отмен", ignoreCase = true)
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val containerColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
    }
    val titleColor = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant
    val statusIcon = when {
        missed -> Icons.Default.PhoneMissed
        isOutgoing || cancelled -> Icons.Default.CallMade
        else -> Icons.Default.CallReceived
    }
    val statusTint = when {
        missed -> MaterialTheme.colorScheme.error
        isOutgoing -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isOutgoing) 20.dp else 8.dp,
                bottomEnd = if (isOutgoing) 8.dp else 20.dp,
            ),
            color = containerColor,
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusTint,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.bodySmall,
                            color = metaColor,
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                ) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = statusTint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatSize(context: android.content.Context, size: Long): String {
    if (size <= 0) return "0 ${context.getString(R.string.size_bytes)}"
    val units = arrayOf(
        context.getString(R.string.size_bytes),
        context.getString(R.string.size_kb),
        context.getString(R.string.size_mb),
        context.getString(R.string.size_gb)
    )
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

@Composable
fun FileTransferCard(
    ft: FileTransfer,
    msg: Message,
    onHaptic: () -> Unit,
    contentColor: Color,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (Message) -> Unit,
    onSaveAsClick: (Int, String) -> Unit,
    onOpenFile: (FileTransfer) -> Unit
) {
    val isComplete = ft.isComplete()
    val isStarted = ft.isStarted()
    val isRejected = ft.isRejected()
    val isOutgoing = ft.outgoing
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isComplete && !isRejected) { onOpenFile(ft) }
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = when {
                    isRejected -> Icons.Default.ErrorOutline
                    isComplete -> Icons.AutoMirrored.Filled.InsertDriveFile
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ft.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        isRejected -> stringResource(R.string.ft_status_canceled)
                        isComplete -> {
                            val status = if (isOutgoing) stringResource(R.string.ft_status_sent) else stringResource(R.string.ft_status_received)
                            "${formatSize(context, ft.fileSize)} • $status"
                        }
                        !isStarted -> {
                            if (isOutgoing) stringResource(R.string.ft_status_waiting) 
                            else stringResource(R.string.ft_status_incoming, formatSize(context, ft.fileSize))
                        }
                        else -> stringResource(R.string.ft_status_progress, formatSize(context, ft.progress), formatSize(context, ft.fileSize))
                    },
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            when {
                isRejected -> {
                    IconButton(
                        onClick = {
                            onHaptic()
                            onCancelFt(msg)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                !isStarted -> {
                    if (isOutgoing) {
                        IconButton(
                            onClick = {
                                onHaptic()
                                onCancelFt(msg)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = contentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Row {
                            IconButton(
                                onClick = {
                                    onHaptic()
                                    onRejectFt(ft.id)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Decline",
                                    tint = contentColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    onHaptic()
                                    onAcceptFt(ft.id)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Accept",
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                !isComplete -> {
                    IconButton(
                        onClick = {
                            onHaptic()
                            onRejectFt(ft.id)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (!isComplete && isStarted && !isRejected) {
            Spacer(modifier = Modifier.height(8.dp))
            val progressFraction = if (ft.fileSize > 0) ft.progress.toFloat() / ft.fileSize.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOutgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                trackColor = contentColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun TypingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha1)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha2)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha3)))
            }
        }
    }
}
