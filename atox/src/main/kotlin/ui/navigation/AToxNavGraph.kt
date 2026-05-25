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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
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
import ltd.evilcorp.atox.util.PermissionManager
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.FINGERPRINT_LEN
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.CallState
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.atox.ui.NotificationHelper

@Composable
@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun AToxNavGraph(
    appearance: AppAppearance,
    settings: Settings,
    vmFactory: ViewModelProvider.Factory,
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
    val mainNavController = rememberNavController()
    val callState by callManager.inCall.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context as? AppCompatActivity }

    val contactListViewModel: ContactListViewModel = if (activity != null) {
        viewModel(viewModelStoreOwner = activity, factory = vmFactory)
    } else {
        viewModel(factory = vmFactory)
    }
    val authViewModel: AuthViewModel = if (activity != null) {
        viewModel(viewModelStoreOwner = activity, factory = vmFactory)
    } else {
        viewModel(factory = vmFactory)
    }
    val selectedChatSnapshot = contactListViewModel.selectedChatSnapshot.collectAsStateWithLifecycle()

    val backgroundColor = MaterialTheme.colorScheme.background
    LaunchedEffect(backgroundColor) {
        activity?.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(backgroundColor.toArgb())
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LaunchedEffect(callState, callScreenMinimized.value) {
            val publicKey = callState.publicKeyForCallScreen()
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (publicKey != null && !callScreenMinimized.value) {
                if (!AppRoutes.isCall(currentRoute)) {
                    navController.navigateSingleTop(AppRoutes.call(publicKey))
                }
            } else if (AppRoutes.isCall(currentRoute)) {
                navController.popBackStack()
            }
        }

        LaunchedEffect(initialToxIdToLink.value) {
            initialToxIdToLink.value?.let { toxId ->
                navController.navigate(AppRoutes.addContact(toxId))
                initialToxIdToLink.value = null
            }
        }

        val currentBackStack by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStack?.destination?.route
        LaunchedEffect(MainActivity.sharedContentState.value, currentRoute) {
            if (MainActivity.sharedContentState.value != null) {
                if (currentRoute != null && currentRoute != AppRoutes.Launch && currentRoute != AppRoutes.Unlock && currentRoute != AppRoutes.CreateProfile) {
                    navController.navigate("chat/forward_shared") {
                        launchSingleTop = true
                    }
                }
            }
        }

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
                authViewModel = authViewModel,
                vmFactory = vmFactory,
                onQuitApp = onQuitApp
            )

            composable(
                route = AppRoutes.ContactList,
                enterTransition = { AToxMotion.sharedAxisXEnter(forward = true) },
                exitTransition = { AToxMotion.sharedAxisXExit(forward = true) },
                popEnterTransition = { AToxMotion.sharedAxisXEnter(forward = false) },
                popExitTransition = { AToxMotion.sharedAxisXExit(forward = false) },
            ) {
                val mainBackStackEntry by mainNavController.currentBackStackEntryAsState()

                val contactsState = contactListViewModel.visibleContacts.collectAsStateWithLifecycle(emptyList())
                val friendRequestsViewModel: FriendRequestsViewModel = viewModel(factory = vmFactory)
                val friendRequestsState = friendRequestsViewModel.friendRequests.collectAsStateWithLifecycle(emptyList())
                val groupInviteState = contactListViewModel.groupInvite.collectAsStateWithLifecycle(null)
                val groupInviteFriendNameState = contactListViewModel.groupInviteFriendName.collectAsStateWithLifecycle("")

                // Lifted TopAppBar & Search states
                val isSearchingState = rememberSaveable { mutableStateOf(false) }
                val settingsTitleState = remember { mutableStateOf("") }
                val settingsOnBackActionState = remember { mutableStateOf<(() -> Unit)?>(null) }
                val settingsOnSearchActionState = remember { mutableStateOf<(() -> Unit)?>(null) }

                val mainRoute = mainBackStackEntry?.destination?.route ?: AppRoutes.Chats
                val coroutineScope = rememberCoroutineScope()

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

                MainTabsScreen(
                    currentRoute = mainRoute,
                    attentionCount = chatListAttentionCount(contactsState.value, friendRequestsState.value),
                    hapticEnabled = settings.hapticEnabled,
                    topBar = reactiveTopBar,
                    onTabSelected = { route ->
                        mainNavController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(AppRoutes.Chats) {
                                saveState = true
                            }
                        }
                    },
                ) {
                    NavHost(
                        navController = mainNavController,
                        startDestination = AppRoutes.Chats,
                        enterTransition = {
                            val initialRoute = initialState.destination.route.orEmpty()
                            val targetRoute = targetState.destination.route.orEmpty()
                            val forward = getRouteOrder(targetRoute) > getRouteOrder(initialRoute)
                            AToxMotion.sharedAxisXEnter(forward = forward)
                        },
                        exitTransition = {
                            val initialRoute = initialState.destination.route.orEmpty()
                            val targetRoute = targetState.destination.route.orEmpty()
                            val forward = getRouteOrder(targetRoute) > getRouteOrder(initialRoute)
                            AToxMotion.sharedAxisXExit(forward = forward)
                        },
                        popEnterTransition = {
                            val initialRoute = initialState.destination.route.orEmpty()
                            val targetRoute = targetState.destination.route.orEmpty()
                            val forward = getRouteOrder(targetRoute) > getRouteOrder(initialRoute)
                            AToxMotion.sharedAxisXEnter(forward = forward)
                        },
                        popExitTransition = {
                            val initialRoute = initialState.destination.route.orEmpty()
                            val targetRoute = targetState.destination.route.orEmpty()
                            val forward = getRouteOrder(targetRoute) > getRouteOrder(initialRoute)
                            AToxMotion.sharedAxisXExit(forward = forward)
                        },
                    ) {
                        mainTabGraph(
                            mainNavController = mainNavController,
                            navController = navController,
                            vmFactory = vmFactory,
                            contactListViewModel = contactListViewModel,
                            settings = settings,
                            appearance = appearance,
                            isSearchingState = isSearchingState,
                            settingsTitleState = settingsTitleState,
                            settingsOnBackActionState = settingsOnBackActionState,
                            settingsOnSearchActionState = settingsOnSearchActionState,
                            contactsState = contactsState,
                            friendRequestsState = friendRequestsState,
                            groupInviteState = groupInviteState,
                            groupInviteFriendNameState = groupInviteFriendNameState,
                            coroutineScope = coroutineScope,
                            onThemeChanged = onThemeChanged,
                            onDynamicColorChanged = onDynamicColorChanged,
                            onAccentColorSeedChanged = onAccentColorSeedChanged,
                            onLocaleTagChanged = onLocaleTagChanged,
                            onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                        )

                        groupGraph(
                            mainNavController = mainNavController,
                            vmFactory = vmFactory,
                            contactListViewModel = contactListViewModel,
                            settings = settings,
                            onOpenFile = onOpenFile,
                            systemSoundPlayer = systemSoundPlayer,
                        )

                        chatGraph(
                            mainNavController = mainNavController,
                            navController = navController,
                            vmFactory = vmFactory,
                            contactListViewModel = contactListViewModel,
                            settings = settings,
                            selectedChatSnapshotState = selectedChatSnapshot,
                            systemSoundPlayer = systemSoundPlayer,
                            onOpenFile = onOpenFile,
                            coroutineScope = coroutineScope,
                        )
                    }
                }
            }

            callGraph(
                navController = navController,
                vmFactory = vmFactory,
                permissionManager = permissionManager,
                callScreenMinimized = callScreenMinimized,
                settings = settings,
            )

            composable(
                route = AppRoutes.ForwardSelection,
                arguments = listOf(navArgument("message") { type = NavType.StringType }),
                enterTransition = { AToxMotion.sharedAxisZEnter(forward = true) },
                exitTransition = { AToxMotion.sharedAxisZExit(forward = true) },
                popEnterTransition = { AToxMotion.sharedAxisZEnter(forward = false) },
                popExitTransition = { AToxMotion.sharedAxisZExit(forward = false) },
            ) { backStackEntry ->
                val messageText = backStackEntry.arguments?.getString("message").orEmpty()
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

            composable(
                route = "chat/forward_shared",
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
                            mainNavController.navigate(AppRoutes.chat(selectedList.first().publicKey)) {
                                popUpTo(AppRoutes.Chats) { inclusive = false }
                            }
                        }
                    }
                )
            }
        }

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

private fun getRouteOrder(key: String): Int {
    return when {
        key.startsWith("tab-0") || key == AppRoutes.Chats || key == "main/chats" -> 0
        key.startsWith("tab-1") || key == AppRoutes.Groups || key == "main/groups" -> 1
        key.startsWith("tab-2") || key == AppRoutes.AddContactTab || key == "main/add_contact" -> 2
        key.startsWith("tab-3") || key == AppRoutes.Profile || key == "main/profile" -> 3
        key.startsWith("tab-4") || key == AppRoutes.Settings || key == "main/settings" -> 4
        key.startsWith("sub-4") -> 5
        key.startsWith("chat/") || key.startsWith("group_chat/") || key.startsWith("create_group") || key.startsWith("join_group") -> 6
        else -> 6
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

// Extension to convert Color to Int for window background color
private fun androidx.compose.ui.graphics.Color.toArgb(): Int {
    return (this.value shr 32).toInt()
}
