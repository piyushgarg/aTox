package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.common.MorphingNavigationIcon
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.groupchat.CreateGroupScreen
import ltd.evilcorp.atox.ui.groupchat.GroupChatScreen
import ltd.evilcorp.atox.ui.groupchat.GroupChatViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.groupchat.JoinGroupScreen
import ltd.evilcorp.atox.ui.navigation.AppBarStateHolder
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.domain.feature.GroupConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.groupGraph(
    mainNavController: NavHostController,
    vmFactory: ViewModelProvider.Factory,
    contactListViewModel: ContactListViewModel,
    settings: Settings,
    onOpenFile: (FileTransfer) -> Unit,
    systemSoundPlayer: SystemSoundPlayer,
) {
    composable(AppRoutes.CreateGroup) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            AppBarStateHolder.config.value = null
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
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
                                onClick = { mainNavController.popBackStack() }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                val groupListViewModel: GroupListViewModel = viewModel(factory = vmFactory)
                CreateGroupScreen(
                    onBack = { mainNavController.popBackStack() },
                    isCreatingState = groupListViewModel.isCreating,
                    onCreateGroup = { name, privacyState, password ->
                        val num = groupListViewModel.createGroup(name, privacyState, password)
                        if (num >= 0) {
                            mainNavController.popBackStack()
                            true
                        } else {
                            false
                        }
                    }
                )
            }
        }
    }

    composable(AppRoutes.JoinGroup) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            AppBarStateHolder.config.value = null
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
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
                                onClick = { mainNavController.popBackStack() }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                val groupListViewModel: GroupListViewModel = viewModel(factory = vmFactory)
                JoinGroupScreen(
                    onBack = { mainNavController.popBackStack() },
                    isJoiningState = groupListViewModel.isJoining,
                    onValidateChatId = { groupListViewModel.validateChatId(it) },
                    onJoinGroup = { chatIdHex, password ->
                        val num = groupListViewModel.joinByChatId(chatIdHex, password)
                        if (num >= 0) {
                            mainNavController.popBackStack()
                            true
                        } else {
                            false
                        }
                    }
                )
            }
        }
    }

    composable(
        route = AppRoutes.GroupChat,
        arguments = listOf(navArgument(AppRoutes.ChatIdArg) { type = NavType.StringType }),
    ) { backStackEntry ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            AppBarStateHolder.config.value = null
        }

        val chatIdStr = backStackEntry.arguments?.getString(AppRoutes.ChatIdArg).orEmpty()
        val viewModel: GroupChatViewModel = viewModel(factory = vmFactory)
        val contactsState = contactListViewModel.contacts.collectAsStateWithLifecycle()
        
        remember(chatIdStr) {
            viewModel.setActiveGroup(chatIdStr)
        }
        
        val groupState = viewModel.group.collectAsStateWithLifecycle()
        val messagesState = viewModel.messages.collectAsStateWithLifecycle()
        val peersState = viewModel.peers.collectAsStateWithLifecycle()
        val connectionStatusState = viewModel.connectionStatus.collectAsStateWithLifecycle()
        val fileTransfersState = viewModel.fileTransfers.collectAsStateWithLifecycle()

        GroupChatScreen(
            groupState = groupState,
            messagesState = messagesState,
            peersState = peersState,
            contactsState = contactsState,
            connectionStatusState = connectionStatusState,
            fileTransfersState = fileTransfersState,
            settings = settings,
            onBack = { mainNavController.popBackStack() },
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
