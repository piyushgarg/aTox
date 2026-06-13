@file:Suppress("WildcardImport")

package ltd.evilcorp.atox.ui.navigation.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.chat.ChatScreen
import ltd.evilcorp.atox.ui.chat.ChatViewModel
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupChatScreen
import ltd.evilcorp.atox.ui.groupchat.GroupChatViewModel
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.ui.navigation.*
import ltd.evilcorp.atox.ui.navigation.graphs.*
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.core.model.PublicKey

private const val LEFT_PANE_WEIGHT = 0.35f
private const val RIGHT_PANE_WEIGHT = 0.65f

@Composable
fun AToxSplitPaneLayout(
    navController: NavHostController,
    navHost: @Composable () -> Unit,
    currentRoute: String?,
    showBottomBar: Boolean,
    attentionCount: Int,
    settings: Settings,
    contactListViewModel: ContactListViewModel,
    selectedChatSnapshot: State<Contact?>,
    systemSoundPlayer: SystemSoundPlayer,
    onOpenFile: (FileTransfer) -> Unit,
) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Pane: 35% width, local Scaffold
        Box(modifier = Modifier.weight(LEFT_PANE_WEIGHT).fillMaxHeight()) {
            Scaffold(
                bottomBar = {
                    AToxBottomBar(
                        currentRoute = currentRoute,
                        visible = showBottomBar,
                        attentionCount = attentionCount,
                        hapticEnabled = settings.hapticEnabled,
                        onTabSelected = { route ->
                            val targetRoute: Any = when (route) {
                                AppRoutes.Chats::class.qualifiedName -> AppRoutes.Chats
                                AppRoutes.Profile::class.qualifiedName -> AppRoutes.Profile
                                AppRoutes.Settings::class.qualifiedName -> AppRoutes.Settings
                                else -> AppRoutes.Chats
                            }
                            navController.navigate(targetRoute) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(AppRoutes.Chats) {
                                    saveState = true
                                }
                            }
                        }
                    )
                },
                floatingActionButton = {
                    AToxFAB(
                        currentRoute = currentRoute,
                        visible = currentRoute?.endsWith("AppRoutes.Chats") == true,
                        hapticEnabled = settings.hapticEnabled,
                        onAddContactClick = {
                            navController.navigate(AppRoutes.AddContactTab) {
                                launchSingleTop = true
                            }
                        },
                        onCreateGroupClick = {
                            navController.navigate(AppRoutes.CreateGroup)
                        },
                        onJoinGroupClick = {
                            navController.navigate(AppRoutes.JoinGroup)
                        }
                    )
                }
            ) { leftPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(leftPadding)) {
                    CompositionLocalProvider(LocalTabPadding provides PaddingValues(bottom = 0.dp)) {
                        navHost()
                    }
                }
            }
        }

        VerticalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Right Pane: 65% width
        Box(modifier = Modifier.weight(RIGHT_PANE_WEIGHT).fillMaxHeight()) {
            val selectedChat = selectedChatSnapshot.value
            val selectedGroupState by contactListViewModel.selectedGroupSnapshot.collectAsStateWithLifecycle()
            val selectedGroup = selectedGroupState
            if (selectedChat != null) {
                val rightChatViewModel: ChatViewModel = hiltViewModel(key = selectedChat.publicKey)
                
                LaunchedEffect(selectedChat.publicKey) {
                    rightChatViewModel.setActiveChat(PublicKey(selectedChat.publicKey))
                }
                
                val uiState by rightChatViewModel.uiState.collectAsStateWithLifecycle()
                val finalUiState = remember(uiState, selectedChat) {
                    if (uiState.contact == null) {
                        uiState.copy(contact = selectedChat)
                    } else {
                        uiState
                    }
                }
                
                val groupListViewModel: GroupListViewModel = hiltViewModel()
                val groupsState by groupListViewModel.groups.collectAsStateWithLifecycle()
                val coroutineScope = rememberCoroutineScope()
                
                ChatScreen(
                    uiState = finalUiState,
                    onBack = { contactListViewModel.clearSelectedChat() },
                    messagesFlow = rightChatViewModel.pagedMessages,
                    onSendMessage = { content -> rightChatViewModel.send(content, ltd.evilcorp.domain.features.chat.model.MessageType.Normal) },
                    onTypingChanged = rightChatViewModel::setTyping,
                    onSendFile = rightChatViewModel::createFt,
                    onCallClick = rightChatViewModel::startCall,
                    onCallHistoryClick = rightChatViewModel::startCall,
                    onAcceptFt = rightChatViewModel::acceptFt,
                    onRejectFt = rightChatViewModel::rejectFt,
                    onCancelFt = rightChatViewModel::delete,
                    onSaveFt = rightChatViewModel::exportFt,
                    onOpenFile = onOpenFile,
                    systemSoundPlayer = systemSoundPlayer,
                    isExpanded = true,
                    voiceRecorder = rightChatViewModel.voiceRecorder,
                    isTypingFlow = rightChatViewModel.isTyping,
                    onCancelReply = { rightChatViewModel.setReplyingTo(null) },
                    onReplyClick = { msg -> rightChatViewModel.setReplyingTo(msg) },
                    onCopyClick = { msg ->
                        val clipboard = context.getSystemService(
                            android.content.Context.CLIPBOARD_SERVICE
                        ) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("aTox message", msg.message)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.message_copied), Toast.LENGTH_SHORT).show()
                    },
                    onForwardClick = { msg ->
                        navController.navigate(AppRoutes.ForwardSelection(msg.message))
                    },
                    onSendVoice = rightChatViewModel::createFt,
                    isJoinedGroup = { chatId ->
                        groupsState.any { it.chatId.equals(chatId, ignoreCase = true) }
                    },
                    onJoinGroupClick = { chatIdOrBytes, groupName ->
                        coroutineScope.launch {
                            val alreadyJoined = groupsState.any { it.chatId.equals(chatIdOrBytes, ignoreCase = true) }
                            if (alreadyJoined) {
                                navController.navigate(AppRoutes.GroupChat(chatIdOrBytes))
                                return@launch
                            }
                            val chatId = groupListViewModel.joinGroupFromChat(
                                friendPublicKey = selectedChat.publicKey,
                                chatIdOrBytes = chatIdOrBytes,
                                groupName = groupName,
                            )
                            if (chatId != null && chatId.isNotEmpty()) {
                                navController.navigate(AppRoutes.GroupChat(chatId))
                            } else {
                                Toast.makeText(context, context.getString(R.string.group_join_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            } else if (selectedGroup != null) {
                val rightGroupChatViewModel: GroupChatViewModel = hiltViewModel(key = selectedGroup.chatId)
                LaunchedEffect(selectedGroup.chatId) {
                    rightGroupChatViewModel.setActiveGroup(selectedGroup.chatId)
                }

                val groupState = rightGroupChatViewModel.group.collectAsStateWithLifecycle()
                val messagesState = rightGroupChatViewModel.messages.collectAsStateWithLifecycle()
                val peersState = rightGroupChatViewModel.peers.collectAsStateWithLifecycle()
                val connectionStatusState = rightGroupChatViewModel.connectionStatus.collectAsStateWithLifecycle()
                val fileTransfersState = rightGroupChatViewModel.fileTransfers.collectAsStateWithLifecycle()
                val selfAvatarUriState = rightGroupChatViewModel.selfAvatarUri.collectAsStateWithLifecycle()
                val contactsState = contactListViewModel.contacts.collectAsStateWithLifecycle()

                val userSettingsState by settings.state.collectAsStateWithLifecycle()
                val uiConfig = remember(userSettingsState) {
                    ChatUiConfig(
                        hapticEnabled = userSettingsState.hapticEnabled,
                        dateFormatPreference = userSettingsState.dateFormatPreference,
                        timeFormatPreference = userSettingsState.timeFormatPreference,
                        sentMessageSoundUri = userSettingsState.sentMessageSoundUri,
                        sentMessageSoundVolume = userSettingsState.sentMessageSoundVolume,
                        enableReplies = userSettingsState.enableReplies,
                    )
                }

                GroupChatScreen(
                    groupState = groupState,
                    messagesState = messagesState,
                    peersState = peersState,
                    contactsState = contactsState,
                    connectionStatusState = connectionStatusState,
                    fileTransfersState = fileTransfersState,
                    selfAvatarUriState = selfAvatarUriState,
                    uiConfig = uiConfig,
                    onBack = { contactListViewModel.clearSelectedGroup() },
                    onSendMessage = { msg -> rightGroupChatViewModel.sendMessage(msg) },
                    onSendFile = { uri -> rightGroupChatViewModel.sendFile(uri) },
                    onSendVoice = { uri -> rightGroupChatViewModel.sendVoice(uri) },
                    onAcceptFt = { ftId -> rightGroupChatViewModel.acceptFt(ftId) },
                    onRejectFt = { ftId -> rightGroupChatViewModel.rejectFt(ftId) },
                    onCancelFt = { msg -> rightGroupChatViewModel.cancelFt(msg) },
                    onSaveAsClick = { ftId, dest -> rightGroupChatViewModel.saveFt(ftId, android.net.Uri.parse(dest)) },
                    onOpenFile = onOpenFile,
                    onLeaveGroup = { rightGroupChatViewModel.leaveGroup() },
                    voiceRecorder = rightGroupChatViewModel.voiceRecorder,
                    onCopyInvite = {
                        val id = rightGroupChatViewModel.getChatId() ?: ""
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("group invite", id)
                        clipboard.setPrimaryClip(clip)
                    },
                    onInviteFriend = { friendPk -> rightGroupChatViewModel.inviteFriend(friendPk) },
                    systemSoundPlayer = systemSoundPlayer,
                    isExpanded = true
                )
            } else {
                PlaceholderScreen()
            }
        }
    }
}
