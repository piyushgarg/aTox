package ltd.evilcorp.atox.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import ltd.evilcorp.atox.ui.navigation.components.IncomingCallOverlay
import ltd.evilcorp.atox.ui.navigation.components.AToxSplitPaneLayout
import ltd.evilcorp.atox.ui.navigation.components.PlaceholderScreen
import ltd.evilcorp.atox.ui.navigation.graphs.authGraph
import ltd.evilcorp.atox.ui.navigation.graphs.callGraph
import ltd.evilcorp.atox.ui.navigation.graphs.mainTabGraph
import ltd.evilcorp.atox.ui.navigation.graphs.groupGraph
import ltd.evilcorp.atox.ui.navigation.graphs.chatGraph
import ltd.evilcorp.atox.infrastructure.util.PermissionManager
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.call.CallManager
import ltd.evilcorp.domain.features.call.CallState
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

    val animatedBottomPadding by animateDpAsState(
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

    val navHostContent = remember(navController, isExpandedMode) {
        androidx.compose.runtime.movableContentOf {
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
                    isExpanded = { isExpandedMode },
                    onThemeChanged = onThemeChanged,
                    onDynamicColorChanged = onDynamicColorChanged,
                    onAccentColorSeedChanged = onAccentColorSeedChanged,
                    onLocaleTagChanged = onLocaleTagChanged,
                    onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                )

                composable<AppRoutes.SearchContacts>(
                    enterTransition = { AToxMotion.slideXEnter(forward = true) },
                    exitTransition = { AToxMotion.slideXExit(forward = true) },
                    popEnterTransition = { AToxMotion.slideXEnter(forward = false) },
                    popExitTransition = { AToxMotion.slideXExit(forward = false) }
                ) {
                    val contacts by contactListViewModel.contacts.collectAsStateWithLifecycle()
                    ltd.evilcorp.atox.ui.contactlist.SearchContactsScreen(
                        contacts = contacts,
                        onContactClick = { contact ->
                            contactListViewModel.prepareOpenChat(contact)
                            navController.navigate(AppRoutes.Chat(contact.publicKey)) {
                                popUpTo(AppRoutes.SearchContacts) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<AppRoutes.SearchSettings>(
                    enterTransition = { AToxMotion.slideXEnter(forward = true) },
                    exitTransition = { AToxMotion.slideXExit(forward = true) },
                    popEnterTransition = { AToxMotion.slideXEnter(forward = false) },
                    popExitTransition = { AToxMotion.slideXExit(forward = false) }
                ) {
                    ltd.evilcorp.atox.ui.settings.SearchSettingsScreen(
                        settings = settings,
                        appearance = appearance,
                        onItemClick = { item ->
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Chat detail
                chatGraph(
                    navController = navController,
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

    Scaffold(
        contentWindowInsets = WindowInsets(0),
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

                // Unified NavHost call
                if (isExpandedMode && showBottomBar) {
                    AToxSplitPaneLayout(
                        navController = navController,
                        navHost = navHostContent,
                        currentRoute = currentRoute,
                        showBottomBar = showBottomBar,
                        attentionCount = attentionCount,
                        settings = settings,
                        contactListViewModel = contactListViewModel,
                        selectedChatSnapshot = selectedChatSnapshot,
                        systemSoundPlayer = systemSoundPlayer,
                        onOpenFile = onOpenFile,
                    )
                } else {
                    CompositionLocalProvider(LocalTabPadding provides tabPadding) {
                        navHostContent()
                    }
                }

                // Incoming call dialog
                IncomingCallOverlay(
                    callState = callState,
                    callManager = callManager,
                    notificationHelper = notificationHelper,
                )
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

