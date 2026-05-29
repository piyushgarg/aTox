package ltd.evilcorp.atox.ui.navigation.graphs

import android.content.ClipData
import android.widget.Toast
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import ltd.evilcorp.atox.ui.navigation.LocalAnimatedVisibilityScope
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.ui.chat.ChatScreen
import ltd.evilcorp.atox.ui.chat.ChatViewModel
import ltd.evilcorp.atox.ui.chat.ChatUiState
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.atox.ui.theme.AToxMotion

private const val CHAT_ID_LENGTH = 64

fun NavGraphBuilder.chatGraph(
    navController: NavHostController,

    selectedChatSnapshotState: State<Contact?>,
    systemSoundPlayer: SystemSoundPlayer,
    onOpenFile: (FileTransfer) -> Unit,
) {
    composable<AppRoutes.Chat>(
        enterTransition = { AToxMotion.slideXEnter(forward = true) },
        exitTransition = { AToxMotion.slideXExit(forward = true) },
        popEnterTransition = { AToxMotion.slideXEnter(forward = false) },
        popExitTransition = { AToxMotion.slideXExit(forward = false) }
    ) { backStackEntry ->
        val chatRoute = backStackEntry.toRoute<AppRoutes.Chat>()
        val publicKeyStr = chatRoute.publicKey
        val viewModel: ChatViewModel = hiltViewModel()
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            viewModel.uiEvents.collect { event ->
                when (event) {
                    is ChatViewModel.ChatUiEvent.ShowToast -> {
                        val text = if (event.formatArg != null) {
                            context.getString(event.messageResId, event.formatArg)
                        } else {
                            context.getString(event.messageResId)
                        }
                        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        DisposableEffect(publicKeyStr) {
            viewModel.setActiveChat(PublicKey(publicKeyStr))
            onDispose {
                viewModel.clearActiveChat(PublicKey(publicKeyStr))
            }
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val selectedChatSnapshot = selectedChatSnapshotState.value
        val contactSnapshot = remember(selectedChatSnapshot, publicKeyStr) {
            selectedChatSnapshot?.takeIf { it.publicKey == publicKeyStr }
        }
        val finalUiState = remember(uiState, contactSnapshot) {
            if (uiState.contact == null && contactSnapshot != null) {
                uiState.copy(contact = contactSnapshot)
            } else {
                uiState
            }
        }
        val groupListViewModel: GroupListViewModel = hiltViewModel()
        val groupsState by groupListViewModel.groups.collectAsStateWithLifecycle()
 
        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            ChatScreen(
                uiState = finalUiState,
                onBack = navController::popBackStack,
                onSendMessage = { content -> viewModel.send(content, MessageType.Normal) },
                onTypingChanged = viewModel::setTyping,
                onSendFile = viewModel::createFt,
                onCallClick = viewModel::startCall,
                onCallHistoryClick = viewModel::startCall,
                onAcceptFt = viewModel::acceptFt,
                onRejectFt = viewModel::rejectFt,
                onCancelFt = viewModel::delete,
                onSaveFt = viewModel::exportFt,
                voiceRecorder = viewModel.voiceRecorder,
                onOpenFile = onOpenFile,
                systemSoundPlayer = systemSoundPlayer,
                isTypingFlow = viewModel.isTyping,
                onCancelReply = { viewModel.setReplyingTo(null) },
                onReplyClick = { msg -> viewModel.setReplyingTo(msg) },
                onCopyClick = { msg ->
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("aTox message", msg.message)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.message_copied), Toast.LENGTH_SHORT).show()
                },
                onForwardClick = { msg ->
                    navController.navigate(AppRoutes.ForwardSelection(msg.message))
                },
                onSendVoice = viewModel::createFt,
                isJoinedGroup = { chatId ->
                    groupsState.any { it.chatId.equals(chatId, ignoreCase = true) }
                },
                onJoinGroupClick = { chatIdOrBytes, groupName ->
                    coroutineScope.launch {
                        val alreadyJoined = groupsState.any { it.chatId.equals(chatIdOrBytes, ignoreCase = true) }
                        if (alreadyJoined) {
                            navController.navigate(AppRoutes.GroupChat(chatIdOrBytes)) {
                                popUpTo(AppRoutes.Chat(publicKeyStr)) { inclusive = true }
                            }
                            return@launch
                        }

                        val groupNumber = if (chatIdOrBytes.length == CHAT_ID_LENGTH) {
                            val pending = groupListViewModel.getPendingInviteDirect()
                            if (pending != null && pending.groupName.equals(groupName, ignoreCase = true)) {
                                 groupListViewModel.joinWithPendingInvite(pending)
                            } else {
                                groupListViewModel.joinByChatId(chatIdOrBytes, null)
                            }
                        } else {
                            groupListViewModel.joinGroupWithBytes(publicKeyStr, chatIdOrBytes, null)
                        }
                        
                        if (groupNumber >= 0) {
                            val chatId = if (chatIdOrBytes.length == CHAT_ID_LENGTH) {
                                chatIdOrBytes
                            } else {
                                groupListViewModel.getChatIdByGroupNumber(groupNumber) ?: ""
                            }
                            if (chatId.isNotEmpty()) {
                                navController.navigate(AppRoutes.GroupChat(chatId)) {
                                    popUpTo(AppRoutes.Chat(publicKeyStr)) { inclusive = true }
                                }
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.group_join_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}
