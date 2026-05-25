package ltd.evilcorp.atox.ui.navigation.graphs

import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.chat.ChatScreen
import ltd.evilcorp.atox.ui.chat.ChatViewModel
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey

fun NavGraphBuilder.chatGraph(
    mainNavController: NavHostController,
    navController: NavHostController,
    vmFactory: ViewModelProvider.Factory,
    contactListViewModel: ContactListViewModel,
    settings: Settings,
    selectedChatSnapshotState: State<Contact?>,
    systemSoundPlayer: SystemSoundPlayer,
    onOpenFile: (FileTransfer) -> Unit,
    coroutineScope: CoroutineScope,
) {
    composable(
        route = AppRoutes.Chat,
        arguments = listOf(navArgument(AppRoutes.PublicKeyArg) { type = NavType.StringType }),
    ) { backStackEntry ->
        val publicKeyStr = backStackEntry.arguments?.getString(AppRoutes.PublicKeyArg).orEmpty()
        val viewModel: ChatViewModel = viewModel(factory = vmFactory)
        val context = LocalContext.current

        remember(publicKeyStr) {
            viewModel.setActiveChat(PublicKey(publicKeyStr))
        }

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
            onDispose {
                viewModel.clearActiveChat(PublicKey(publicKeyStr))
            }
        }

        val contact by viewModel.contact.collectAsStateWithLifecycle()
        val selectedChatSnapshot = selectedChatSnapshotState.value
        val contactSnapshot = remember(selectedChatSnapshot, publicKeyStr) {
            selectedChatSnapshot?.takeIf { it.publicKey == publicKeyStr }
        }
        val messages by viewModel.messages.collectAsStateWithLifecycle()
        val fileTransfers by viewModel.fileTransfers.collectAsStateWithLifecycle(emptyList())
        val replyingToMessage by viewModel.replyingToMessage.collectAsStateWithLifecycle()
        val groupListViewModel: GroupListViewModel = viewModel(factory = vmFactory)
        val groupsState by groupListViewModel.groups.collectAsStateWithLifecycle()

        ChatScreen(
            contact = contact ?: contactSnapshot,
            messages = messages,
            fileTransfers = fileTransfers,
            settings = settings,
            onBack = mainNavController::popBackStack,
            onSendMessage = { content -> viewModel.send(content, MessageType.Normal) },
            onTypingChanged = viewModel::setTyping,
            onSendFile = viewModel::createFt,
            onCallClick = viewModel::startCall,
            onCallHistoryClick = viewModel::startCall,
            onAcceptFt = viewModel::acceptFt,
            onRejectFt = viewModel::rejectFt,
            onCancelFt = viewModel::delete,
            onSaveFt = viewModel::exportFt,
            onOpenFile = onOpenFile,
            systemSoundPlayer = systemSoundPlayer,
            replyingToMessage = replyingToMessage,
            onCancelReply = { viewModel.setReplyingTo(null) },
            onReplyClick = { msg -> viewModel.setReplyingTo(msg) },
            onCopyClick = { msg ->
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("aTox message", msg.message)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.message_copied), Toast.LENGTH_SHORT).show()
            },
            onForwardClick = { msg ->
                navController.navigate(AppRoutes.forwardSelection(msg.message))
            },
            onSendVoice = viewModel::createFt,
            isJoinedGroup = { chatId ->
                groupsState.any { it.chatId.equals(chatId, ignoreCase = true) }
            },
            onJoinGroupClick = { chatIdOrBytes, groupName ->
                coroutineScope.launch {
                    val alreadyJoined = groupsState.any { it.chatId.equals(chatIdOrBytes, ignoreCase = true) }
                    if (alreadyJoined) {
                        mainNavController.navigate(AppRoutes.groupChat(chatIdOrBytes)) {
                            popUpTo(AppRoutes.chat(publicKeyStr)) { inclusive = true }
                        }
                        return@launch
                    }

                    val groupNumber = if (chatIdOrBytes.length == 64) {
                        val pending = groupListViewModel.getPendingInvite()
                        if (pending != null && pending.groupName.equals(groupName, ignoreCase = true)) {
                            groupListViewModel.joinWithPendingInvite(publicKeyStr, pending)
                        } else {
                            groupListViewModel.joinByChatId(chatIdOrBytes, null)
                        }
                    } else {
                        groupListViewModel.joinGroupWithBytes(publicKeyStr, chatIdOrBytes, null)
                    }
                    
                    if (groupNumber >= 0) {
                        val chatId = if (chatIdOrBytes.length == 64) {
                            chatIdOrBytes
                        } else {
                            groupListViewModel.getChatIdByGroupNumber(groupNumber) ?: ""
                        }
                        if (chatId.isNotEmpty()) {
                            mainNavController.navigate(AppRoutes.groupChat(chatId)) {
                                popUpTo(AppRoutes.chat(publicKeyStr)) { inclusive = true }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Не удалось вступить в группу", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}
