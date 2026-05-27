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
import ltd.evilcorp.atox.ui.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.chat.ForwardSelectionScreen
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.contactlist.components.chatListAttentionCount
import ltd.evilcorp.atox.ui.navigation.components.ReturnToCallBanner
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

@Suppress("FunctionNaming", "ViewModelInjection", "CyclomaticComplexMethod", "MaxLineLength")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AToxNavGraph(
    appearance: AppAppearance,
    settings: Settings,
    callManager: CallManager,
    notificationHelper: NotificationHelper,
    permissionManager: PermissionManager,
    systemSoundPlayer: SystemSoundPlayer,
    initialToxIdToLink: MutableState<String?>,
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
    val authViewModel: AuthViewModel = hiltViewModel()
    val selectedChatSnapshot = contactListViewModel.selectedChatSnapshot.collectAsStateWithLifecycle()

    val contactsState by contactListViewModel.visibleContacts.collectAsStateWithLifecycle(emptyList())
    val friendRequestsViewModel: FriendRequestsViewModel = hiltViewModel()
    val friendRequestsState by friendRequestsViewModel.friendRequests.collectAsStateWithLifecycle(emptyList())
    val attentionCount = chatListAttentionCount(contactsState, friendRequestsState)

    // Track current route for reactive UI
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // Update AppBarStateHolder with current route
    LaunchedEffect(currentRoute) {
        AppBarStateHolder.updateRoute(currentRoute)
    }

    // Reactive TopAppBar driven by AppBarStateHolder
    val reactiveTopBar: @Composable () -> Unit = {
        val currentConfig by AppBarStateHolder.config.collectAsStateWithLifecycle()
        currentConfig?.let { cfg ->
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

    val targetBottomPadding = if (isSubScreen || !showBottomBar) {
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
                topBar = reactiveTopBar,
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
                        visible = showBottomBar && (currentRoute?.endsWith("AppRoutes.Chats") == true || currentRoute?.endsWith("AppRoutes.Groups") == true),
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
                    LaunchedEffect(initialToxIdToLink.value) {
                        initialToxIdToLink.value?.let { toxId ->
                            navController.navigate(AppRoutes.AddContact(toxId))
                            initialToxIdToLink.value = null
                        }
                    }

                    // Shared content handling
                    LaunchedEffect(MainActivity.sharedContentState.value, currentRoute) {
                        if (MainActivity.sharedContentState.value != null) {
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
                    CompositionLocalProvider(LocalTabPadding provides tabPadding) {
                        NavHost(
                            navController = navController,
                            startDestination = AppRoutes.Launch,
                            enterTransition = { AToxMotion.sharedAxisZEnter(forward = true) },
                            exitTransition = { AToxMotion.sharedAxisZExit(forward = true) },
                            popEnterTransition = { AToxMotion.sharedAxisZEnter(forward = false) },
                            popExitTransition = { AToxMotion.sharedAxisZExit(forward = false) },
                        ) {
                            // Auth flow
                            authGraph(
                                navController = navController,
                                authViewModel = authViewModel,
                                onQuitApp = onQuitApp
                            )

                            // Main tab destinations (flat, no nesting)
                            mainTabGraph(
                                navController = navController,
                                contactListViewModel = contactListViewModel,
                                settings = settings,
                                appearance = appearance,
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
                                settings = settings,
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

                            // Forward selection
                            composable<AppRoutes.ForwardSelection>(
                                enterTransition = { AToxMotion.sharedAxisZEnter(forward = true) },
                                exitTransition = { AToxMotion.sharedAxisZExit(forward = true) },
                                popEnterTransition = { AToxMotion.sharedAxisZEnter(forward = false) },
                                popExitTransition = { AToxMotion.sharedAxisZExit(forward = false) },
                            ) { backStackEntry ->
                                val forwardRoute = backStackEntry.toRoute<AppRoutes.ForwardSelection>()
                                val messageText = forwardRoute.message
                                val contactsState by contactListViewModel.contacts.collectAsStateWithLifecycle()
                                val ctx = LocalContext.current

                                ForwardSelectionScreen(
                                    contacts = contactsState,
                                    settings = settings,
                                    onBack = { navController.popBackStack() },
                                    onContactsSelect = { selectedList ->
                                        selectedList.forEach { contact ->
                                            contactListViewModel.onShareText(messageText, contact)
                                        }
                                        Toast.makeText(ctx, ctx.getString(R.string.message_forwarded), Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                )
                            }

                            // Shared content forwarding
                            composable<AppRoutes.ForwardShared>(
                                enterTransition = { AToxMotion.sharedAxisZEnter(forward = true) },
                                exitTransition = { AToxMotion.sharedAxisZExit(forward = true) },
                                popEnterTransition = { AToxMotion.sharedAxisZEnter(forward = false) },
                                popExitTransition = { AToxMotion.sharedAxisZExit(forward = false) },
                            ) {
                                val contactsState by contactListViewModel.contacts.collectAsStateWithLifecycle()
                                val ctx = LocalContext.current

                                ForwardSelectionScreen(
                                    contacts = contactsState,
                                    settings = settings,
                                    onBack = {
                                        MainActivity.sharedContentState.value = null
                                        navController.popBackStack()
                                    },
                                    onContactsSelect = { selectedList ->
                                        val content = MainActivity.sharedContentState.value
                                        if (content != null) {
                                            selectedList.forEach { contact ->
                                                when (content) {
                                                    is SharedContent.Text -> {
                                                        contactListViewModel.onShareText(content.text, contact)
                                                    }
                                                    is SharedContent.File -> {
                                                        contactListViewModel.onShareFile(content.uri, contact)
                                                    }
                                                    is SharedContent.MultipleFiles -> {
                                                        content.uris.forEach { uri ->
                                                            contactListViewModel.onShareFile(uri, contact)
                                                        }
                                                    }
                                                }
                                            }
                                            if (content is SharedContent.Text) {
                                                Toast.makeText(ctx, ctx.getString(R.string.message_forwarded), Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(ctx, ctx.getString(R.string.file_sharing_started), Toast.LENGTH_SHORT).show()
                                            }
                                            MainActivity.sharedContentState.value = null
                                        }
                                        navController.popBackStack()
                                        if (selectedList.size == 1) {
                                            navController.navigate(AppRoutes.Chat(selectedList.first().publicKey)) {
                                                popUpTo(AppRoutes.Chats) { inclusive = false }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                // Return to call banner
                val publicKey = callState.publicKeyForCallScreen()
                if (callScreenMinimized.value && publicKey != null) {
                    ReturnToCallBanner(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(1f)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                            .padding(top = 72.dp, start = 16.dp, end = 16.dp),
                        onClick = {
                            callScreenMinimized.value = false
                        },
                    )
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
