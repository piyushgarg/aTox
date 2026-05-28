@file:Suppress("WildcardImport", "MagicNumber")

package ltd.evilcorp.atox.ui.navigation.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.infrastructure.util.PermissionManager
import ltd.evilcorp.atox.ui.chat.ChatScreen
import ltd.evilcorp.atox.ui.chat.ChatViewModel
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.navigation.*
import ltd.evilcorp.atox.ui.navigation.graphs.*
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.core.model.PublicKey

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
        Box(modifier = Modifier.weight(0.35f).fillMaxHeight()) {
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
                                AppRoutes.Groups::class.qualifiedName -> AppRoutes.Groups
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
                        visible = currentRoute?.endsWith("AppRoutes.Chats") == true || currentRoute?.endsWith("AppRoutes.Groups") == true,
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
        Box(modifier = Modifier.weight(0.65f).fillMaxHeight()) {
            val selectedChat = selectedChatSnapshot.value
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
                                Toast.makeText(context, "Не удалось вступить в группу", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            } else {
                PlaceholderScreen()
            }
        }
    }
}
