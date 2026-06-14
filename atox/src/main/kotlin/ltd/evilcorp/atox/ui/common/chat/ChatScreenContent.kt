@file:Suppress("MatchingDeclarationName")

package ltd.evilcorp.atox.ui.common.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.paging.compose.LazyPagingItems
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.ui.chat.components.TypingBubble
import ltd.evilcorp.atox.ui.common.formatMessageDateHeader
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.chat.model.Message

data class MessageBubbleConfig(
    val contactName: String,
    val showAvatar: Boolean = false,
    val senderName: String? = null,
    val senderColor: Color? = null,
    val avatarUri: String = "",
)

@Suppress("FunctionNaming", "UnstableCollections", "ComposableParamOrder")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> ChatScreenContent(
    messages: List<T>,
    toMessage: (T) -> Message,
    getBubbleConfig: (T) -> MessageBubbleConfig,
    uiConfig: ChatUiConfig,
    fileTransfers: List<FileTransfer>,
    onSendMessage: (String) -> Unit,
    onTypingChanged: (Boolean) -> Unit,
    onSendFile: (Uri) -> Unit,
    onSendVoice: (Uri) -> Unit,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (T) -> Unit,
    onSaveFt: (Int, Uri) -> Unit,
    onOpenFile: (FileTransfer) -> Unit,
    systemSoundPlayer: SystemSoundPlayer,
    performHaptic: () -> Unit,
    voiceRecorder: ltd.evilcorp.domain.features.call.service.IVoiceRecorder,

    // Bottom input bar configurations
    contact: Contact?, // Passing null works for group chat
    replyingToMessage: Message? = null,
    onCancelReply: () -> Unit = {},

    // Optional parameters
    modifier: Modifier = Modifier,
    pagedMessages: LazyPagingItems<T>? = null,
    isTypingFlow: StateFlow<Boolean>? = null, // null for group chats
    showConversationContent: Boolean = true,
    onCallHistoryClick: () -> Unit = {},
    onCopyClick: (Message) -> Unit = {},
    onReplyClick: (Message) -> Unit = {},
    onForwardClick: (Message) -> Unit = {},
    onJoinGroupClick: (String, String) -> Unit = { _, _ -> },
    isJoinedGroup: (String) -> Boolean = { false },
    listState: LazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val showScrollToBottomFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    val mappedMessages = remember(messages) {
        messages.map(toMessage)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSendFile(uri)
        }
    }

    val activeFtIdToSave = remember { mutableIntStateOf(-1) }
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null && activeFtIdToSave.intValue != -1) {
            onSaveFt(activeFtIdToSave.intValue, uri)
        }
    }

    var lastSeenMessageId by remember { mutableStateOf<Long?>(null) }
    var unreadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemIndex, mappedMessages.size) {
        if (listState.firstVisibleItemIndex <= 1) {
            unreadCount = 0
            lastSeenMessageId = mappedMessages.firstOrNull()?.id
        } else {
            val lastId = lastSeenMessageId
            if (lastId != null) {
                val index = mappedMessages.indexOfFirst { it.id == lastId }
                unreadCount = if (index > 0) index else 0
            }
        }
    }

    val currentMessagesCount = pagedMessages?.itemCount ?: messages.size
    LaunchedEffect(showConversationContent, currentMessagesCount) {
        if (showConversationContent && currentMessagesCount > 0) {
            if (listState.firstVisibleItemIndex <= 2) {
                listState.scrollToItem(0)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
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
                if (isTypingFlow != null) {
                    item(key = "typing_bubble") {
                        val isTyping by isTypingFlow.collectAsState(initial = false)
                        Column(modifier = Modifier.animateContentSize()) {
                            AnimatedVisibility(
                                visible = showConversationContent && isTyping
                            ) {
                                TypingBubble()
                            }
                        }
                    }
                }

                if (showConversationContent) {
                    val itemCount = pagedMessages?.itemCount ?: messages.size
                    items(
                        count = itemCount,
                        key = { index ->
                            if (pagedMessages != null) {
                                pagedMessages.peek(index)?.let(toMessage)?.id ?: index.toLong()
                            } else {
                                val rawIndex = itemCount - 1 - index
                                messages.getOrNull(rawIndex)?.let(toMessage)?.id ?: index.toLong()
                            }
                        }
                    ) { index ->
                        val rawIndex = itemCount - 1 - index
                        val item = if (pagedMessages != null) {
                            pagedMessages[index]
                        } else {
                            messages[rawIndex]
                        }
                        if (item != null) {
                            val msg = toMessage(item)
                            val prevDomainMsg = if (pagedMessages != null) {
                                if (index + 1 < itemCount) {
                                    pagedMessages.peek(index + 1)?.let(toMessage)
                                } else null
                            } else {
                                if (rawIndex > 0) {
                                    messages.getOrNull(rawIndex - 1)?.let(toMessage)
                                } else null
                            }

                                val currentHeader = remember(msg.timestamp, uiConfig.dateFormatPreference) {
                                    formatMessageDateHeader(
                                        context = context,
                                        timestamp = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp,
                                        dateFormatPreference = uiConfig.dateFormatPreference,
                                    )
                                }
                                val previousHeader = prevDomainMsg?.let {
                                    remember(it.timestamp, uiConfig.dateFormatPreference) {
                                        formatMessageDateHeader(
                                            context = context,
                                            timestamp = if (it.timestamp == 0L) System.currentTimeMillis() else it.timestamp,
                                            dateFormatPreference = uiConfig.dateFormatPreference,
                                        )
                                    }
                                }

                                val bubbleConfig = remember(item) { getBubbleConfig(item) }

                                @OptIn(ExperimentalFoundationApi::class)
                                Column(modifier = Modifier.animateItem()) {
                                    if (rawIndex == 0 || currentHeader != previousHeader) {
                                        DateSeparator(label = currentHeader)
                                        if (bubbleConfig.showAvatar) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }

                                    MessageBubble(
                                        msg = msg,
                                        messages = StableMessageList(mappedMessages),
                                        uiConfig = uiConfig,
                                        contactName = bubbleConfig.contactName,
                                        onHaptic = performHaptic,
                                        onCallHistoryClick = onCallHistoryClick,
                                        fileTransfers = StableFileTransferList(fileTransfers),
                                        onAcceptFt = onAcceptFt,
                                        onRejectFt = onRejectFt,
                                        onCancelFt = { onCancelFt(item) },
                                        onSaveAsClick = { ftId, fileName ->
                                            activeFtIdToSave.intValue = ftId
                                            saveFileLauncher.launch(fileName)
                                        },
                                        onOpenFile = onOpenFile,
                                        onCopyMessage = onCopyClick,
                                        onReplyMessage = onReplyClick,
                                        onForwardMessage = onForwardClick,
                                        onParentMessageClick = { parentMsg ->
                                            val parentIndex = mappedMessages.indexOfFirst { it.timestamp == parentMsg.timestamp }
                                            if (parentIndex != -1) {
                                                val scrollIndex = mappedMessages.size - 1 - parentIndex
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(scrollIndex)
                                                }
                                            }
                                        },
                                        onJoinGroupClick = onJoinGroupClick,
                                        isJoinedGroup = isJoinedGroup,
                                        showAvatar = bubbleConfig.showAvatar,
                                        senderName = bubbleConfig.senderName,
                                        senderColor = bubbleConfig.senderColor,
                                        avatarUri = bubbleConfig.avatarUri
                                    )
                                }
                        }
                    }
                }
            }

            if (showScrollToBottomFab) {
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
                    if (unreadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(unreadCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to Bottom",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
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
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            ) {
                ChatInputBar(
                    contact = contact,
                    uiConfig = uiConfig,
                    systemSoundPlayer = systemSoundPlayer,
                    onSendMessage = onSendMessage,
                    onTypingChanged = onTypingChanged,
                    onAttachClick = { filePickerLauncher.launch("*/*") },
                    onHaptic = performHaptic,
                    replyingToMessage = replyingToMessage,
                    onCancelReply = onCancelReply,
                    onSendVoice = onSendVoice,
                    voiceRecorder = voiceRecorder,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
