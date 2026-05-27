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
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.ui.common.ContactAvatar
import androidx.compose.animation.ExperimentalSharedTransitionApi
import ltd.evilcorp.atox.ui.navigation.LocalSharedTransitionScope
import ltd.evilcorp.atox.ui.navigation.LocalAnimatedVisibilityScope
import ltd.evilcorp.atox.ui.common.MorphingNavigationIcon
import ltd.evilcorp.atox.ui.common.PresenceTone
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.atox.ui.common.formatMessageDateHeader
import ltd.evilcorp.atox.ui.common.formatPresenceText
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusBusy
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.model.DateFormatPreference
import ltd.evilcorp.domain.model.TimeFormatPreference
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Error
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.domain.model.FT_NOT_STARTED
import ltd.evilcorp.domain.model.FT_REJECTED
import ltd.evilcorp.domain.model.isComplete
import ltd.evilcorp.domain.model.isStarted
import ltd.evilcorp.domain.model.isRejected
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.atox.ui.common.chat.ChatScreenContent
import ltd.evilcorp.atox.ui.common.chat.MessageBubbleConfig
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import ltd.evilcorp.atox.ui.navigation.AppBarStateHolder
import ltd.evilcorp.atox.ui.navigation.AppBarConfig
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.common.AtoxAppBar
import ltd.evilcorp.atox.ui.common.AtoxConfirmDialog

private const val CHAT_ENTER_CONTENT_DELAY_MS = 320L

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Suppress("LongMethod", "FunctionNaming")
@Composable
fun ChatScreen(
    uiState: ChatUiState,
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
    isExpanded: Boolean = false,
    onCancelReply: () -> Unit = {},
    onReplyClick: (Message) -> Unit = {},
    onCopyClick: (Message) -> Unit = {},
    onForwardClick: (Message) -> Unit = {},
    onSendVoice: (Uri) -> Unit = {},
    onJoinGroupClick: (String, String) -> Unit = { _, _ -> },
    isJoinedGroup: (String) -> Boolean = { false },
    isTypingFlow: StateFlow<Boolean> = remember(uiState.contact?.publicKey) { MutableStateFlow(uiState.contact?.typing == true) },
) {
    val contact = uiState.contact
    val messages = uiState.messages
    val fileTransfers = uiState.fileTransfers
    val replyingToMessage = uiState.replyingToMessage
    val uiConfig = uiState.uiConfig ?: ChatUiConfig(
        hapticEnabled = false,
        dateFormatPreference = DateFormatPreference.System,
        timeFormatPreference = TimeFormatPreference.System,
        sentMessageSoundUri = "",
        sentMessageSoundVolume = 24,
        enableReplies = true
    )
    var showConversationContent by remember(contact?.publicKey) { mutableStateOf(true) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val performHaptic = {
        if (uiConfig.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

    var showCallConfirmDialog by remember { mutableStateOf(false) }

    if (showCallConfirmDialog) {
        AtoxConfirmDialog(
            onDismiss = {
                performHaptic()
                showCallConfirmDialog = false
            },
            onConfirm = {
                performHaptic()
                showCallConfirmDialog = false
                onCallClick()
            },
            title = stringResource(R.string.incoming_call),
            text = stringResource(R.string.call_confirm),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.reject)
        )
    }

    val contactName = contact?.name?.ifEmpty { context.getString(R.string.contact_default_name) } ?: context.getString(R.string.contact_default_name)
    val presenceInfo = contact?.let {
        formatPresenceText(
            context = context,
            contact = it,
            dateFormatPreference = uiConfig.dateFormatPreference,
            timeFormatPreference = uiConfig.timeFormatPreference
        )
    }
    val connectionStatus = contact?.connectionStatus
    val userStatus = contact?.status
    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    val topAppBarTitle = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        performHaptic()
                        val pk = contact?.publicKey ?: ""
                        if (pk.isNotEmpty()) {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("friend ID", pk)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(
                                context,
                                context.getString(R.string.profile_copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
        ) {
            val avatarModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && contact != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = "avatar_${contact.publicKey}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            } else {
                Modifier
            }

            ContactAvatar(
                name = contactName,
                publicKey = contact?.publicKey ?: "",
                avatarUri = contact?.avatarUri ?: "",
                size = 36.dp,
                fontSize = 14.sp,
                modifier = avatarModifier
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (connectionStatus) {
                                    ltd.evilcorp.domain.model.ConnectionStatus.None -> ltd.evilcorp.atox.ui.theme.StatusOffline
                                    else -> when (userStatus) {
                                        ltd.evilcorp.domain.model.UserStatus.Away -> ltd.evilcorp.atox.ui.theme.StatusAway
                                        ltd.evilcorp.domain.model.UserStatus.Busy -> ltd.evilcorp.atox.ui.theme.StatusBusy
                                        else -> ltd.evilcorp.atox.ui.theme.StatusAvailable
                                    }
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = presenceInfo?.text ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    val topAppBarNavigationIcon = @Composable {
        Box(modifier = Modifier.padding(start = 4.dp)) {
            MorphingNavigationIcon(
                isBack = true,
                onClick = {
                    performHaptic()
                    onBack()
                }
            )
        }
    }

    val topAppBarActions = @Composable {
        IconButton(onClick = {
            performHaptic()
            onCallClick()
        }) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    val content = @Composable { paddingValues: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ChatScreenContent(
                messages = messages,
                toMessage = { it },
                getBubbleConfig = {
                    MessageBubbleConfig(
                        contactName = contactName
                    )
                },
                uiConfig = uiConfig,
                fileTransfers = fileTransfers,
                onSendMessage = onSendMessage,
                onTypingChanged = onTypingChanged,
                onSendFile = onSendFile,
                onSendVoice = onSendVoice,
                onAcceptFt = onAcceptFt,
                onRejectFt = onRejectFt,
                onCancelFt = onCancelFt,
                onSaveFt = onSaveFt,
                onOpenFile = onOpenFile,
                systemSoundPlayer = systemSoundPlayer,
                performHaptic = performHaptic,
                contact = contact,
                replyingToMessage = replyingToMessage,
                onCancelReply = onCancelReply,
                isTypingFlow = isTypingFlow,
                showConversationContent = showConversationContent,
                onCallHistoryClick = onCallHistoryClick,
                onCopyClick = onCopyClick,
                onReplyClick = onReplyClick,
                onForwardClick = onForwardClick,
                onJoinGroupClick = onJoinGroupClick,
                isJoinedGroup = isJoinedGroup
            )
        }
    }

    if (isExpanded) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = topAppBarTitle,
                    navigationIcon = topAppBarNavigationIcon,
                    actions = { topAppBarActions() },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = surfaceContainerColor,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                )
            }
        ) { paddingValues ->
            content(paddingValues)
        }
    } else {
        AtoxAppBar(
            route = AppRoutes.Chat::class.qualifiedName!!,
            config = AppBarConfig(
                title = topAppBarTitle,
                navigationIcon = topAppBarNavigationIcon,
                actions = { topAppBarActions() },
                containerColor = surfaceContainerColor
            )
        )

        content(PaddingValues(0.dp))
    }
}
