package ltd.evilcorp.atox.ui.chat

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import androidx.compose.animation.ExperimentalSharedTransitionApi
import ltd.evilcorp.atox.ui.common.formatPresenceText
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import android.net.Uri
import androidx.activity.compose.BackHandler
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.atox.ui.common.chat.ChatScreenContent
import ltd.evilcorp.atox.ui.common.chat.MessageBubbleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import ltd.evilcorp.atox.ui.chat.components.CallConfirmDialog
import ltd.evilcorp.atox.ui.chat.components.ChatAppBar
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyListState


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
    messagesFlow: Flow<PagingData<Message>>? = null,
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
    voiceRecorder: ltd.evilcorp.domain.features.call.service.IVoiceRecorder,
) {
    val contact = uiState.contact
    val messages = uiState.messages
    val pagedMessages = messagesFlow?.collectAsLazyPagingItems()
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

    val performHaptic = {
        if (uiConfig.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

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
    @Suppress("DEPRECATION")
    val originalNavBarColor = remember(activity) { activity?.window?.navigationBarColor ?: 0 }
    @Suppress("DEPRECATION")
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
        CallConfirmDialog(
            onDismiss = {
                performHaptic()
                showCallConfirmDialog = false
            },
            onConfirm = {
                performHaptic()
                showCallConfirmDialog = false
                onCallClick()
            }
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

    val content = @Composable { paddingValues: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            ChatScreenContent(
                messages = messages,
                toMessage = { it },
                pagedMessages = pagedMessages,
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
                voiceRecorder = voiceRecorder,
                showConversationContent = showConversationContent,
                onCallHistoryClick = onCallHistoryClick,
                onCopyClick = onCopyClick,
                onReplyClick = onReplyClick,
                onForwardClick = onForwardClick,
                onJoinGroupClick = onJoinGroupClick,
                isJoinedGroup = isJoinedGroup,
                listState = listState
            )
        }
    }

    Scaffold(
        topBar = {
            ChatAppBar(
                contact = contact,
                contactName = contactName,
                presenceInfoText = presenceInfo?.text ?: "",
                connectionStatus = connectionStatus,
                userStatus = userStatus,
                isExpanded = isExpanded,
                transitionElevation = transitionElevation,
                transitionAlpha = transitionAlpha,
                onBack = onBack,
                onCallClick = onCallClick,
                performHaptic = performHaptic
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}
