package ltd.evilcorp.atox.ui.navigation.graphs

import android.content.ClipData
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.common.MorphingNavigationIcon
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.groupchat.CreateGroupScreen
import ltd.evilcorp.atox.ui.groupchat.GroupChatScreen
import ltd.evilcorp.atox.ui.groupchat.GroupChatViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.ui.groupchat.JoinGroupScreen
import ltd.evilcorp.atox.ui.navigation.AppBarStateHolder
import ltd.evilcorp.atox.ui.navigation.AppBarConfig
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.domain.model.FileTransfer

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.groupGraph(
    navController: NavHostController,

    contactListViewModel: ContactListViewModel,
    settings: Settings,
    onOpenFile: (FileTransfer) -> Unit,
    systemSoundPlayer: SystemSoundPlayer,
) {
    composable<AppRoutes.CreateGroup> {
        val context = LocalContext.current
        val containerColor = MaterialTheme.colorScheme.surfaceContainer
        val createGroupRouteName = AppRoutes.CreateGroup::class.qualifiedName!!

        LaunchedEffect(Unit) {
            AppBarStateHolder.register(
                route = createGroupRouteName,
                cfg = AppBarConfig(
                    title = {
                        Text(
                            text = context.getString(R.string.create_group),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        Box(modifier = Modifier.padding(start = 4.dp)) {
                            MorphingNavigationIcon(
                                isBack = true,
                                onClick = { navController.popBackStack() }
                            )
                        }
                    },
                    containerColor = containerColor
                )
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                AppBarStateHolder.unregister(createGroupRouteName)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val groupListViewModel: GroupListViewModel = hiltViewModel()
            CreateGroupScreen(
                onBack = { navController.popBackStack() },
                isCreatingState = groupListViewModel.isCreating,
                onCreateGroup = { name, privacyState, password ->
                    val num = groupListViewModel.createGroup(name, privacyState, password)
                    if (num >= 0) {
                        navController.popBackStack()
                        true
                    } else {
                        false
                    }
                }
            )
        }
    }

    composable<AppRoutes.JoinGroup> {
        val context = LocalContext.current
        val containerColor = MaterialTheme.colorScheme.surfaceContainer
        val joinGroupRouteName = AppRoutes.JoinGroup::class.qualifiedName!!

        LaunchedEffect(Unit) {
            AppBarStateHolder.register(
                route = joinGroupRouteName,
                cfg = AppBarConfig(
                    title = {
                        Text(
                            text = context.getString(R.string.join_group),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        Box(modifier = Modifier.padding(start = 4.dp)) {
                            MorphingNavigationIcon(
                                isBack = true,
                                onClick = { navController.popBackStack() }
                            )
                        }
                    },
                    containerColor = containerColor
                )
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                AppBarStateHolder.unregister(joinGroupRouteName)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val groupListViewModel: GroupListViewModel = hiltViewModel()
            JoinGroupScreen(
                onBack = { navController.popBackStack() },
                isJoiningState = groupListViewModel.isJoining,
                onValidateChatId = { groupListViewModel.validateChatId(it) },
                onJoinGroup = { chatIdHex, password ->
                    val num = groupListViewModel.joinByChatId(chatIdHex, password)
                    if (num >= 0) {
                        navController.popBackStack()
                        true
                    } else {
                        false
                    }
                }
            )
        }
    }

    composable<AppRoutes.GroupChat> { backStackEntry ->
        val context = LocalContext.current
        val groupChatRoute = backStackEntry.toRoute<AppRoutes.GroupChat>()
        val chatIdStr = groupChatRoute.chatId

        val viewModel: GroupChatViewModel = hiltViewModel()
        val contactsState = contactListViewModel.contacts.collectAsStateWithLifecycle()
        
        DisposableEffect(chatIdStr) {
            viewModel.setActiveGroup(chatIdStr)
            onDispose {}
        }
        
        val groupState = viewModel.group.collectAsStateWithLifecycle()
        val messagesState = viewModel.messages.collectAsStateWithLifecycle()
        val peersState = viewModel.peers.collectAsStateWithLifecycle()
        val connectionStatusState = viewModel.connectionStatus.collectAsStateWithLifecycle()
        val fileTransfersState = viewModel.fileTransfers.collectAsStateWithLifecycle()
        val selfAvatarUriState = viewModel.selfAvatarUri.collectAsStateWithLifecycle()

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
            onBack = { navController.popBackStack() },
            onSendMessage = { msg -> viewModel.sendMessage(msg) },
            onSendFile = { uri -> viewModel.sendFile(uri) },
            onSendVoice = { uri -> viewModel.sendVoice(uri) },
            onAcceptFt = { ftId -> viewModel.acceptFt(ftId) },
            onRejectFt = { ftId -> viewModel.rejectFt(ftId) },
            onCancelFt = { msg -> viewModel.cancelFt(msg) },
            onSaveAsClick = { ftId, dest -> viewModel.saveFt(ftId, android.net.Uri.parse(dest)) },
            onOpenFile = onOpenFile,
            onLeaveGroup = { viewModel.leaveGroup() },
            onCopyInvite = {
                val id = viewModel.getChatId() ?: ""
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("group invite", id)
                clipboard.setPrimaryClip(clip)
            },
            onInviteFriend = { friendPk ->
                viewModel.inviteFriend(friendPk)
            },
            systemSoundPlayer = systemSoundPlayer,
        )
    }
}
