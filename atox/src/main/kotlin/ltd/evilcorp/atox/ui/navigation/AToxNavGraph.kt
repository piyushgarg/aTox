package ltd.evilcorp.atox.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.MainActivity
import ltd.evilcorp.atox.SharedContent
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.navigation.graphs.sharingGraph
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.contactlist.components.chatListAttentionCount
import ltd.evilcorp.atox.ui.navigation.components.AToxWindowDecorator
import ltd.evilcorp.atox.ui.navigation.components.PlaceholderScreen
import ltd.evilcorp.atox.ui.chat.ChatViewModel
import ltd.evilcorp.atox.ui.chat.ChatScreen
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.navigation.graphs.authGraph
import ltd.evilcorp.atox.ui.navigation.graphs.callGraph
import ltd.evilcorp.atox.ui.navigation.graphs.mainTabGraph
import ltd.evilcorp.atox.ui.navigation.graphs.groupGraph
import ltd.evilcorp.atox.ui.navigation.graphs.chatGraph
import ltd.evilcorp.atox.ui.friendrequest.FriendRequestsViewModel
import ltd.evilcorp.atox.infrastructure.util.PermissionManager
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.model.FINGERPRINT_LEN
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.CallState
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.atox.ui.NotificationHelper
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.toRoute
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

@Suppress("FunctionNaming", "ViewModelInjection", "CyclomaticComplexMethod", "MagicNumber")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AToxNavGraph(
    appearance: AppAppearance,
    settings: Settings,
    windowSizeClass: WindowSizeClass,
    callManager: CallManager,
    notificationHelper: NotificationHelper,
    permissionManager: PermissionManager,
    systemSoundPlayer: SystemSoundPlayer,
    toxLinkManager: ToxLinkManager,
    callScreenMinimized: MutableState<Boolean>,
    onOpenFile: (FileTransfer) -> Unit,
    onQuitApp: () -> Unit,
    onThemeChanged: (Int) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onAccentColorSeedChanged: (Int) -> Unit,
    onLocaleTagChanged: (String) -> Unit,
    onDisableScreenshotsChanged: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val isExpandedMode = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val callState by callManager.inCall.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context as? AppCompatActivity }

    val backgroundColor = MaterialTheme.colorScheme.background
    LaunchedEffect(backgroundColor) {
        activity?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(backgroundColor.toArgb())
        )
    }

    val contactListViewModel: ContactListViewModel = hiltViewModel()
    val selectedChatSnapshot = contactListViewModel.selectedChatSnapshot.collectAsStateWithLifecycle()
    val attentionCount by contactListViewModel.attentionCount.collectAsStateWithLifecycle(0)
    val sharedContent by contactListViewModel.sharedContent.collectAsStateWithLifecycle()

    // Track current route for reactive UI
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // Update AppBarStateHolder with current route
    androidx.compose.runtime.SideEffect {
        AppBarStateHolder.updateRoute(currentRoute)
    }

    // Reactive TopAppBar driven by AppBarStateHolder
    val reactiveTopBar: @Composable () -> Unit = {
        val currentConfig by AppBarStateHolder.config.collectAsStateWithLifecycle()
        AnimatedContent(
            targetState = currentConfig,
            transitionSpec = {
                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
            },
            label = "topBarTransition"
        ) { cfg ->
            if (cfg != null) {
                if (cfg.isLarge) {
                    LargeTopAppBar(
                        title = cfg.title,
                        navigationIcon = cfg.navigationIcon ?: {},
                        actions = cfg.actions ?: {},
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = cfg.containerColor ?: MaterialTheme.colorScheme.surfaceContainer,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        scrollBehavior = cfg.scrollBehavior
                    )
                } else {
                    TopAppBar(
                        title = cfg.title,
                        navigationIcon = cfg.navigationIcon ?: {},
                        actions = cfg.actions ?: {},
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = cfg.containerColor ?: MaterialTheme.colorScheme.surfaceContainer,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        scrollBehavior = cfg.scrollBehavior
                    )
                }
            }
        }
    }

    // Determine bottom bar and FAB visibility based on current route
    val showBottomBar = remember(currentRoute) {
        AppRoutes.isMainTab(currentRoute)
    }

    val isSubScreen = remember(currentRoute) {
        currentRoute != null && (
            currentRoute.contains("AppRoutes.Chat") || 
            currentRoute.contains("AppRoutes.GroupChat") ||
            currentRoute.contains("AppRoutes.CreateGroup") ||
            currentRoute.contains("AppRoutes.JoinGroup") ||
            AppRoutes.isCall(currentRoute)
        )
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val navigationBarsInsets = WindowInsets.navigationBars

    val targetBottomPadding = if (isSubScreen || !showBottomBar || isExpandedMode) {
        0.dp
    } else {
        80.dp + with(density) { navigationBarsInsets.getBottom(density).toDp() }
    }

    val animatedBottomPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetBottomPadding,
        animationSpec = tween(
            durationMillis = 300,
            easing = AToxMotion.EmphasizedDecelerate
        ),
        label = "bottomPaddingAnimation"
    )

    val tabPadding = PaddingValues(
        bottom = animatedBottomPadding
    )

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            Scaffold(
                contentWindowInsets = WindowInsets(0),
                topBar = {
                    val showRootTopBar = if (isExpandedMode) !showBottomBar else true
                    if (showRootTopBar) {
                        reactiveTopBar()
                    }
                },
                bottomBar = {
                    AToxBottomBar(
                        currentRoute = currentRoute,
                        visible = showBottomBar && !isExpandedMode,
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
                        visible = showBottomBar && !isExpandedMode && (
                            currentRoute?.endsWith("AppRoutes.Chats") == true ||
                            currentRoute?.endsWith("AppRoutes.Groups") == true
                        ),
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
                        },
                        modifier = Modifier
                    )
                }
            ) { paddingValues ->
                AToxWindowDecorator(
                    callScreenMinimized = callScreenMinimized,
                    publicKeyForCall = callState.publicKeyForCallScreen()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(top = paddingValues.calculateTopPadding())
                    ) {
                    // Call state management
                    LaunchedEffect(callState, callScreenMinimized.value) {
                        val publicKey = callState.publicKeyForCallScreen()
                        val route = navController.currentBackStackEntry?.destination?.route
                        if (publicKey != null && !callScreenMinimized.value) {
                            if (!AppRoutes.isCall(route)) {
                                navController.navigateSingleTop(AppRoutes.Call(publicKey))
                            }
                        } else if (AppRoutes.isCall(route)) {
                            navController.popBackStack()
                        }
                    }

                    // Tox ID deep link handling
                    val pendingToxId by toxLinkManager.pendingToxId.collectAsStateWithLifecycle()
                    LaunchedEffect(pendingToxId) {
                        pendingToxId?.let { toxId ->
                            navController.navigate(AppRoutes.AddContact(toxId))
                            toxLinkManager.clear()
                        }
                    }

                    // Shared content handling
                    LaunchedEffect(sharedContent, currentRoute) {
                        if (sharedContent != null) {
                            val isAuthRoute = currentRoute?.endsWith("AppRoutes.Launch") == true ||
                                              currentRoute?.endsWith("AppRoutes.Unlock") == true ||
                                              currentRoute?.endsWith("AppRoutes.CreateProfile") == true
                            if (currentRoute != null && !isAuthRoute) {
                                navController.navigate(AppRoutes.ForwardShared) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    // Single flat NavHost with ALL destinations
                    if (isExpandedMode && showBottomBar) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Pane: 35% width, local Scaffold
                            Box(modifier = Modifier.weight(0.35f).fillMaxHeight()) {
                                Scaffold(
                                    topBar = {
                                        val leftConfig by AppBarStateHolder.config.collectAsStateWithLifecycle()
                                        LocalTopAppBar(leftConfig)
                                    },
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
                                            NavHost(
                                                navController = navController,
                                                startDestination = AppRoutes.Launch,
                                                enterTransition = { AToxMotion.sharedAxisZEnter(forward = true) },
                                                exitTransition = { AToxMotion.sharedAxisZExit(forward = true) },
                                                popEnterTransition = { AToxMotion.sharedAxisZEnter(forward = false) },
                                                popExitTransition = { AToxMotion.sharedAxisZExit(forward = false) },
                                            ) {
                                                authGraph(navController = navController, onQuitApp = onQuitApp)
                                                mainTabGraph(
                                                    navController = navController,
                                                    contactListViewModel = contactListViewModel,
                                                    settings = settings,
                                                    appearance = appearance,
                                                    isExpanded = true,
                                                    onThemeChanged = onThemeChanged,
                                                    onDynamicColorChanged = onDynamicColorChanged,
                                                    onAccentColorSeedChanged = onAccentColorSeedChanged,
                                                    onLocaleTagChanged = onLocaleTagChanged,
                                                    onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                                                )
                                                chatGraph(
                                                    navController = navController,
                                                    contactListViewModel = contactListViewModel,
                                                    selectedChatSnapshotState = selectedChatSnapshot,
                                                    systemSoundPlayer = systemSoundPlayer,
                                                    onOpenFile = onOpenFile,
                                                )
                                                groupGraph(
                                                    navController = navController,
                                                    contactListViewModel = contactListViewModel,
                                                    settings = settings,
                                                    onOpenFile = onOpenFile,
                                                    systemSoundPlayer = systemSoundPlayer,
                                                )
                                                callGraph(
                                                    navController = navController,
                                                    permissionManager = permissionManager,
                                                    callScreenMinimized = callScreenMinimized,
                                                    settings = settings,
                                                )
                                                sharingGraph(
                                                    navController = navController,
                                                    contactListViewModel = contactListViewModel,
                                                    settings = settings,
                                                )
                                            }
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
                                        onSendMessage = { content -> rightChatViewModel.send(content, ltd.evilcorp.domain.model.MessageType.Normal) },
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
                                                val groupNumber = if (chatIdOrBytes.length == 64) {
                                                    val pending = groupListViewModel.getPendingInvite()
                                                    if (pending != null && pending.groupName.equals(groupName, ignoreCase = true)) {
                                                        groupListViewModel.joinWithPendingInvite(selectedChat.publicKey, pending)
                                                    } else {
                                                        groupListViewModel.joinByChatId(chatIdOrBytes, null)
                                                    }
                                                } else {
                                                    groupListViewModel.joinGroupWithBytes(selectedChat.publicKey, chatIdOrBytes, null)
                                                }
                                                if (groupNumber >= 0) {
                                                    val chatId = if (chatIdOrBytes.length == 64) {
                                                        chatIdOrBytes
                                                    } else {
                                                        groupListViewModel.getChatIdByGroupNumber(groupNumber) ?: ""
                                                    }
                                                    if (chatId.isNotEmpty()) {
                                                        navController.navigate(AppRoutes.GroupChat(chatId))
                                                    }
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
                    } else {
                        CompositionLocalProvider(LocalTabPadding provides tabPadding) {
                            NavHost(
                                navController = navController,
                                startDestination = AppRoutes.Launch,
                                enterTransition = { AToxMotion.sharedAxisZEnter(forward = true) },
                                exitTransition = { AToxMotion.sharedAxisZExit(forward = true) },
                                popEnterTransition = { AToxMotion.sharedAxisZEnter(forward = false) },
                                popExitTransition = { AToxMotion.sharedAxisZExit(forward = false) },
                            ) {
                                authGraph(
                                    navController = navController,
                                    onQuitApp = onQuitApp
                                )

                                // Main tab destinations (flat, no nesting)
                                mainTabGraph(
                                    navController = navController,
                                    contactListViewModel = contactListViewModel,
                                    settings = settings,
                                    appearance = appearance,
                                    isExpanded = false,
                                    onThemeChanged = onThemeChanged,
                                    onDynamicColorChanged = onDynamicColorChanged,
                                    onAccentColorSeedChanged = onAccentColorSeedChanged,
                                    onLocaleTagChanged = onLocaleTagChanged,
                                    onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                                )

                                // Chat detail
                                chatGraph(
                                    navController = navController,
                                    contactListViewModel = contactListViewModel,
                                    selectedChatSnapshotState = selectedChatSnapshot,
                                    systemSoundPlayer = systemSoundPlayer,
                                    onOpenFile = onOpenFile,
                                )

                                // Group screens
                                groupGraph(
                                    navController = navController,
                                    contactListViewModel = contactListViewModel,
                                    settings = settings,
                                    onOpenFile = onOpenFile,
                                    systemSoundPlayer = systemSoundPlayer,
                                )

                                // Call overlay
                                callGraph(
                                    navController = navController,
                                    permissionManager = permissionManager,
                                    callScreenMinimized = callScreenMinimized,
                                    settings = settings,
                                )

                                // Sharing & Forwarding graph
                                sharingGraph(
                                    navController = navController,
                                    contactListViewModel = contactListViewModel,
                                    settings = settings,
                                )
                            }
                        }
                    }

                // Incoming call dialog
                val incomingCall = callState as? CallState.IncomingRinging
                if (incomingCall != null) {
                    val contact = incomingCall.contact
                    val coroutineScope = rememberCoroutineScope()
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(stringResource(R.string.incoming_call)) },
                        text = {
                            Text(
                                stringResource(
                                    R.string.incoming_call_from,
                                    contact.name.ifEmpty { contact.publicKey.take(FINGERPRINT_LEN) }
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val pk = PublicKey(contact.publicKey)
                                        if (callManager.acceptIncomingCall(pk)) {
                                            notificationHelper.showOngoingCallNotification(contact)
                                            notificationHelper.dismissCallNotification(pk)
                                            callManager.startSendingAudio()
                                        }
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.accept))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val pk = PublicKey(contact.publicKey)
                                        callManager.endCall(pk)
                                        notificationHelper.dismissCallNotification(pk)
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.reject))
                            }
                        }
                    )
                }
                }
                }
            }
        }
    }
}

private fun CallState.publicKeyForCallScreen(): String? {
    return when (this) {
        is CallState.OutgoingRequesting -> publicKey.string()
        is CallState.OutgoingWaiting -> publicKey.string()
        is CallState.Connecting -> publicKey.string()
        is CallState.OutgoingRinging -> publicKey.string()
        is CallState.Active -> publicKey.string()
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalTopAppBar(cfg: AppBarConfig?) {
    if (cfg != null) {
        if (cfg.isLarge) {
            LargeTopAppBar(
                title = cfg.title,
                navigationIcon = cfg.navigationIcon ?: {},
                actions = cfg.actions ?: {},
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = cfg.containerColor ?: MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = cfg.scrollBehavior
            )
        } else {
            TopAppBar(
                title = cfg.title,
                navigationIcon = cfg.navigationIcon ?: {},
                actions = cfg.actions ?: {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cfg.containerColor ?: MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = cfg.scrollBehavior
            )
        }
    }
}
