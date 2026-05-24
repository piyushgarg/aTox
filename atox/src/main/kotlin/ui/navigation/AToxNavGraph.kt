package ltd.evilcorp.atox.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusOffline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.livedata.observeAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.media.SystemSoundPlayer
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.addcontact.AddContactScreen
import ltd.evilcorp.atox.ui.addcontact.AddContactViewModel
import ltd.evilcorp.atox.ui.call.CallScreen
import ltd.evilcorp.atox.ui.call.CallViewModel
import ltd.evilcorp.atox.ui.chat.ChatScreen
import ltd.evilcorp.atox.ui.chat.ChatViewModel
import ltd.evilcorp.atox.ui.chat.ForwardSelectionScreen
import ltd.evilcorp.atox.ui.contactlist.ChatsRouteScreen
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.contactlist.components.chatListAttentionCount
import ltd.evilcorp.atox.MainActivity
import ltd.evilcorp.atox.SharedContent
import ltd.evilcorp.atox.ui.navigation.AuthViewModel
import ltd.evilcorp.atox.ui.friendrequest.FriendRequestsViewModel
import ltd.evilcorp.atox.ui.createprofile.CreateProfileScreen
import ltd.evilcorp.atox.ui.createprofile.CreateProfileViewModel
import ltd.evilcorp.atox.ui.settings.SettingsScreen
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.atox.ui.userprofile.UserProfileScreen
import ltd.evilcorp.atox.ui.userprofile.UserProfileViewModel
import ltd.evilcorp.atox.ui.userprofile.AvatarCropUiState
import ltd.evilcorp.atox.util.PermissionManager
import ltd.evilcorp.domain.feature.GroupInvite
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.tox.ToxID
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.CallState
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupChatViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupListScreen
import ltd.evilcorp.atox.ui.groupchat.GroupChatScreen
import ltd.evilcorp.atox.ui.groupchat.CreateGroupScreen
import ltd.evilcorp.atox.ui.groupchat.JoinGroupScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.SizeTransform
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import ltd.evilcorp.atox.ui.common.MorphingNavigationIcon
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.atox.ui.common.PresenceTone
import ltd.evilcorp.atox.ui.common.formatPresenceText
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusBusy
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.model.FINGERPRINT_LEN
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.ui.navigation.components.ReturnToCallBanner

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
    val selectedChatSnapshot by contactListViewModel.selectedChatSnapshot.collectAsStateWithLifecycle()

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
            composable(AppRoutes.Launch) {
                LaunchScreen(
                    viewModel = authViewModel,
                    onLaunchResolved = { status ->
                        val target = when (status) {
                            ToxSaveStatus.Ok -> AppRoutes.ContactList
                            ToxSaveStatus.Encrypted -> AppRoutes.Unlock
                            else -> AppRoutes.CreateProfile
                        }
                        navController.navigate(target) {
                            popUpTo(AppRoutes.Launch) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoutes.Unlock) {
                UnlockScreen(
                    viewModel = authViewModel,
                    onUnlockSuccess = {
                        navController.navigate(AppRoutes.ContactList) {
                            popUpTo(AppRoutes.Unlock) { inclusive = true }
                        }
                    },
                    onQuit = onQuitApp
                )
            }

            composable(
                route = AppRoutes.ContactList,
                enterTransition = { AToxMotion.sharedAxisXEnter(forward = true) },
                exitTransition = { AToxMotion.sharedAxisXExit(forward = true) },
                popEnterTransition = { AToxMotion.sharedAxisXEnter(forward = false) },
                popExitTransition = { AToxMotion.sharedAxisXExit(forward = false) },
            ) {
                val profileViewModel: UserProfileViewModel = viewModel(factory = vmFactory)
                val addContactViewModel: AddContactViewModel = viewModel(factory = vmFactory)
                val friendRequestsViewModel: FriendRequestsViewModel = viewModel(factory = vmFactory)
                val mainBackStackEntry by mainNavController.currentBackStackEntryAsState()

                val userState = contactListViewModel.user.collectAsStateWithLifecycle()
                val contactsState = contactListViewModel.visibleContacts.collectAsStateWithLifecycle(emptyList())
                val friendRequestsState = friendRequestsViewModel.friendRequests.collectAsStateWithLifecycle(emptyList())
                val searchQuery by contactListViewModel.searchQuery.collectAsStateWithLifecycle()
                val groupInviteState = contactListViewModel.groupInvite.collectAsStateWithLifecycle(null)
                val groupInviteFriendNameState = contactListViewModel.groupInviteFriendName.collectAsStateWithLifecycle("")

                // Lifted isSearching state
                var isSearching by rememberSaveable { mutableStateOf(false) }
                var settingsTitle by remember { mutableStateOf("") }
                var settingsOnBackAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                var settingsOnSearchAction by remember { mutableStateOf<(() -> Unit)?>(null) }

                // Group-related TopAppBar state
                var groupOnInviteClick by remember { mutableStateOf<(() -> Unit)?>(null) }
                var groupOnPeersClick by remember { mutableStateOf<(() -> Unit)?>(null) }
                var groupOnLeaveClick by remember { mutableStateOf<(() -> Unit)?>(null) }
                var activeGroupName by remember { mutableStateOf("") }
                var activeGroupTopic by remember { mutableStateOf("") }
                var activeGroupPeerCount by remember { mutableStateOf(0) }
                var activeGroupConnectionStatus by remember { mutableStateOf(GroupConnectionStatus.Disconnected) }

                val mainRoute = mainBackStackEntry?.destination?.route ?: AppRoutes.Chats
                val isChatRoute = mainRoute.startsWith("chat/")
                val isGroupChatRoute = mainRoute.startsWith("group_chat/")

                val currentContact = remember(contactsState.value, mainRoute, selectedChatSnapshot) {
                    if (isChatRoute) {
                        val pk = mainBackStackEntry?.arguments?.getString(AppRoutes.PublicKeyArg)
                        contactsState.value.find { it.publicKey == pk } ?: selectedChatSnapshot?.takeIf { it.publicKey == pk }
                    } else {
                        null
                    }
                }

                val coroutineScope = rememberCoroutineScope()

                val sharedTopBar: @Composable () -> Unit = {
                    val isSupportedRoute = mainRoute == AppRoutes.Chats || 
                                           mainRoute == AppRoutes.Groups || 
                                           mainRoute == AppRoutes.AddContactTab || 
                                           mainRoute == AppRoutes.Profile || 
                                           mainRoute == AppRoutes.Settings ||
                                           mainRoute == AppRoutes.CreateGroup ||
                                           mainRoute == AppRoutes.JoinGroup ||
                                           isChatRoute ||
                                           isGroupChatRoute
                    if (isSupportedRoute) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            navigationIcon = {
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val targetKey = if (isChatRoute) {
                                    currentContact?.publicKey ?: "chat_loading"
                                } else if (isGroupChatRoute) {
                                    "group_chat"
                                } else if (mainRoute == AppRoutes.Settings) {
                                    if (settingsOnBackAction == null) "tab-4" else "sub-4-$settingsTitle"
                                } else if (mainRoute == AppRoutes.CreateGroup) {
                                    "create_group"
                                } else if (mainRoute == AppRoutes.JoinGroup) {
                                    "join_group"
                                } else {
                                    when (mainRoute) {
                                        AppRoutes.Chats -> "tab-0"
                                        AppRoutes.Groups -> "tab-1"
                                        AppRoutes.AddContactTab -> "tab-2"
                                        AppRoutes.Profile -> "tab-3"
                                        else -> mainRoute
                                    }
                                }

                                AnimatedContent(
                                    targetState = targetKey,
                                    transitionSpec = {
                                        val initialOrder = getRouteOrder(initialState)
                                        val targetOrder = getRouteOrder(targetState)
                                        val forward = targetOrder > initialOrder

                                        val isChatTransition = initialOrder == 6 || targetOrder == 6
                                        
                                        val slideOffsetPx = with(density) { 20.dp.roundToPx() }

                                        val slideDistance = if (isChatTransition) {
                                            { fullWidth: Int -> if (forward) fullWidth else -fullWidth }
                                        } else {
                                            { _: Int -> if (forward) slideOffsetPx else -slideOffsetPx }
                                        }

                                        val slideOutDistance = if (isChatTransition) {
                                            { fullWidth: Int -> if (forward) -fullWidth else fullWidth }
                                        } else {
                                            { _: Int -> if (forward) -slideOffsetPx else slideOffsetPx }
                                        }

                                        (slideInHorizontally(initialOffsetX = slideDistance, animationSpec = tween(300, easing = AToxMotion.EmphasizedDecelerate)) + fadeIn(animationSpec = tween(220))) togetherWith
                                        (slideOutHorizontally(targetOffsetX = slideOutDistance, animationSpec = tween(250, easing = AToxMotion.EmphasizedAccelerate)) + fadeOut(animationSpec = tween(150))) using
                                        SizeTransform(clip = false)
                                    },
                                    contentAlignment = Alignment.Center,
                                    label = "TopAppBarNavigationIcon"
                                ) { key ->
                                    when {
                                        key.startsWith("tab-0") || key.startsWith("tab-4") -> {
                                            Box(modifier = Modifier.padding(start = 4.dp)) {
                                                MorphingNavigationIcon(
                                                    isBack = false,
                                                    onClick = {
                                                        if (key.startsWith("tab-0")) {
                                                            isSearching = true
                                                        } else {
                                                            settingsOnSearchAction?.invoke()
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        key.startsWith("tab-1") || key.startsWith("tab-2") || key.startsWith("tab-3") -> {
                                            Spacer(modifier = Modifier.size(0.dp))
                                        }
                                        key.startsWith("sub-4") -> {
                                            IconButton(onClick = { settingsOnBackAction?.invoke() }) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back"
                                                )
                                            }
                                        }
                                        else -> {
                                            Box(modifier = Modifier.padding(start = 4.dp)) {
                                                MorphingNavigationIcon(
                                                    isBack = true,
                                                    onClick = { mainNavController.popBackStack() }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            title = {
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val targetKey = if (isChatRoute) {
                                    currentContact?.publicKey ?: "chat_loading"
                                } else if (isGroupChatRoute) {
                                    "group_chat"
                                } else if (mainRoute == AppRoutes.Settings) {
                                    if (settingsOnBackAction == null) "tab-4" else "sub-4-$settingsTitle"
                                } else if (mainRoute == AppRoutes.CreateGroup) {
                                    "create_group"
                                } else if (mainRoute == AppRoutes.JoinGroup) {
                                    "join_group"
                                } else {
                                    when (mainRoute) {
                                        AppRoutes.Chats -> "tab-0"
                                        AppRoutes.Groups -> "tab-1"
                                        AppRoutes.AddContactTab -> "tab-2"
                                        AppRoutes.Profile -> "tab-3"
                                        else -> mainRoute
                                    }
                                }

                                AnimatedContent(
                                    targetState = targetKey,
                                    transitionSpec = {
                                        val initialOrder = getRouteOrder(initialState)
                                        val targetOrder = getRouteOrder(targetState)
                                        val forward = targetOrder > initialOrder

                                        val isChatTransition = initialOrder == 6 || targetOrder == 6
                                        
                                        val slideOffsetPx = with(density) { 20.dp.roundToPx() }

                                        val slideDistance = if (isChatTransition) {
                                            { fullWidth: Int -> if (forward) fullWidth else -fullWidth }
                                        } else {
                                            { _: Int -> if (forward) slideOffsetPx else -slideOffsetPx }
                                        }

                                        val slideOutDistance = if (isChatTransition) {
                                            { fullWidth: Int -> if (forward) -fullWidth else fullWidth }
                                        } else {
                                            { _: Int -> if (forward) -slideOffsetPx else slideOffsetPx }
                                        }

                                        (slideInHorizontally(initialOffsetX = slideDistance, animationSpec = tween(300, easing = AToxMotion.EmphasizedDecelerate)) + fadeIn(animationSpec = tween(220))) togetherWith
                                        (slideOutHorizontally(targetOffsetX = slideOutDistance, animationSpec = tween(250, easing = AToxMotion.EmphasizedAccelerate)) + fadeOut(animationSpec = tween(150))) using
                                        SizeTransform(clip = false)
                                    },
                                    contentAlignment = Alignment.CenterStart,
                                    label = "TopAppBarTitle"
                                ) { key ->
                                    val contact = currentContact
                                    if (isChatRoute && contact != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ContactAvatar(
                                                name = contact.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                                publicKey = contact.publicKey,
                                                avatarUri = contact.avatarUri,
                                                size = 36.dp,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = contact.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val presenceText = formatPresenceText(
                                                    context = LocalContext.current,
                                                    contact = contact,
                                                    dateFormatPreference = settings.dateFormatPreference,
                                                    timeFormatPreference = settings.timeFormatPreference
                                                )
                                                val presenceColor = when (presenceText.color) {
                                                    PresenceTone.Online -> StatusAvailable
                                                    PresenceTone.Away -> StatusAway
                                                    PresenceTone.Busy -> StatusBusy
                                                    PresenceTone.Accent -> MaterialTheme.colorScheme.primary
                                                    PresenceTone.Muted -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                                Text(
                                                    text = presenceText.text,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = presenceColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    } else if (isGroupChatRoute) {
                                        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onClick = {},
                                                    onLongClick = {
                                                        if (settings.hapticEnabled) {
                                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                        }
                                                        val activeGroupChatId = mainBackStackEntry?.arguments?.getString(AppRoutes.ChatIdArg).orEmpty()
                                                        if (activeGroupChatId.isNotEmpty()) {
                                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                            val clip = android.content.ClipData.newPlainText("group ID", activeGroupChatId)
                                                            clipboard.setPrimaryClip(clip)
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                context.getString(R.string.group_invite_copied),
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Group,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = activeGroupName.ifEmpty { stringResource(R.string.contact_default_name) },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val dotColor = when (activeGroupConnectionStatus) {
                                                        GroupConnectionStatus.Connected -> StatusAvailable
                                                        GroupConnectionStatus.Connecting,
                                                        GroupConnectionStatus.Reconnecting -> StatusAway
                                                        GroupConnectionStatus.Disconnected -> StatusOffline
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(dotColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    val statusText = when (activeGroupConnectionStatus) {
                                                        GroupConnectionStatus.Connected -> stringResource(R.string.group_connected)
                                                        GroupConnectionStatus.Connecting,
                                                        GroupConnectionStatus.Reconnecting -> stringResource(R.string.group_connecting)
                                                        GroupConnectionStatus.Disconnected -> stringResource(R.string.group_offline)
                                                    }
                                                    Text(
                                                        text = "$statusText • ${stringResource(R.string.group_peer_count, activeGroupPeerCount)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        val displayTitle = when {
                                            key.startsWith("tab-0") -> stringResource(R.string.app_name)
                                            key.startsWith("tab-1") -> stringResource(R.string.groups)
                                            key.startsWith("tab-2") -> stringResource(R.string.add_contact)
                                            key.startsWith("tab-3") -> stringResource(R.string.profile)
                                            key.startsWith("tab-4") -> stringResource(R.string.settings)
                                            key.startsWith("sub-4") -> settingsTitle
                                            key == "create_group" -> stringResource(R.string.create_group)
                                            key == "join_group" -> stringResource(R.string.join_group)
                                            else -> stringResource(R.string.app_name)
                                        }
                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            actions = {
                                AnimatedContent(
                                    targetState = if (isGroupChatRoute) "group_actions" else currentContact?.publicKey,
                                    transitionSpec = {
                                        (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(220, easing = AToxMotion.EmphasizedDecelerate)) + fadeIn(animationSpec = tween(220))) togetherWith
                                        (slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(150, easing = AToxMotion.EmphasizedAccelerate)) + fadeOut(animationSpec = tween(150))) using
                                        SizeTransform(clip = false)
                                    },
                                    contentAlignment = Alignment.Center,
                                    label = "TopAppBarActions"
                                ) { target ->
                                    if (target == "group_actions") {
                                        var groupMenuExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(onClick = { groupMenuExpanded = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "More options",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = groupMenuExpanded,
                                                onDismissRequest = { groupMenuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Пригласить друга") },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                                                    },
                                                    onClick = {
                                                        groupMenuExpanded = false
                                                        groupOnInviteClick?.invoke()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Список участников") },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Person, contentDescription = null)
                                                    },
                                                    onClick = {
                                                        groupMenuExpanded = false
                                                        groupOnPeersClick?.invoke()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Выйти из группы", color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                                    },
                                                    onClick = {
                                                        groupMenuExpanded = false
                                                        groupOnLeaveClick?.invoke()
                                                    }
                                                )
                                            }
                                        }
                                    } else if (target != null && currentContact != null) {
                                        val contact = currentContact
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val contactPk = PublicKey(contact.publicKey)
                                                        if (callManager.startOutgoingCall(contactPk)) {
                                                            callManager.startSendingAudio()
                                                            notificationHelper.showOngoingCallNotification(contact)
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = "Call",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { /* Placeholder action menu */ }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "More options",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                MainTabsScreen(
                    currentRoute = mainRoute,
                    attentionCount = chatListAttentionCount(contactsState.value, friendRequestsState.value),
                    hapticEnabled = settings.hapticEnabled,
                    topBar = sharedTopBar,
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
                        composable(
                            route = AppRoutes.Chats
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = LocalTabPadding.current.calculateBottomPadding())
                            ) {
                                ChatsRouteScreen(
                                    contacts = contactsState.value,
                                    friendRequests = friendRequestsState.value,
                                    groupInvite = groupInviteState.value,
                                    groupInviteFriendName = groupInviteFriendNameState.value,
                                    searchQuery = searchQuery,
                                    onSearchQueryChanged = contactListViewModel::setSearchQuery,
                                    dateFormatPreference = settings.dateFormatPreference,
                                    timeFormatPreference = settings.timeFormatPreference,
                                    onContactClick = { contact ->
                                        contactListViewModel.prepareOpenChat(contact)
                                        mainNavController.navigate(AppRoutes.chat(contact.publicKey))
                                    },
                                    onDeleteContact = { contact -> contactListViewModel.deleteContact(PublicKey(contact.publicKey)) },
                                    onAcceptFriendRequest = { req -> friendRequestsViewModel.acceptFriendRequest(req) },
                                    onRejectFriendRequest = { req -> friendRequestsViewModel.rejectFriendRequest(req) },
                                    onAcceptGroupInvite = contactListViewModel::acceptGroupInvite,
                                    onRejectGroupInvite = contactListViewModel::declineGroupInvite,
                                    onAddContactClick = {
                                        mainNavController.navigate(AppRoutes.AddContactTab) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onContactInteraction = {},
                                    isSearching = isSearching,
                                    onSearchingChanged = { isSearching = it }
                                )
                            }
                        }

                        composable(AppRoutes.Groups) {
                            val groupListViewModel: GroupListViewModel = viewModel(factory = vmFactory)
                            val groupsState = groupListViewModel.groups.collectAsStateWithLifecycle()
                            val connectionStatusesState = groupListViewModel.connectionStatuses.collectAsStateWithLifecycle()
                            val scope = rememberCoroutineScope()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = LocalTabPadding.current.calculateBottomPadding())
                            ) {
                                GroupListScreen(
                                    groupsState = groupsState,
                                    connectionStatusesState = connectionStatusesState,
                                    onGroupClick = { group ->
                                        mainNavController.navigate(AppRoutes.groupChat(group.chatId))
                                    },
                                    onCreateGroupClick = {
                                        mainNavController.navigate(AppRoutes.CreateGroup)
                                    },
                                    onJoinGroupClick = {
                                        mainNavController.navigate(AppRoutes.JoinGroup)
                                    },
                                    onLeaveGroup = { group ->
                                        scope.launch {
                                            groupListViewModel.leaveGroup(group)
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoutes.CreateGroup) {
                            val groupListViewModel: GroupListViewModel = viewModel(factory = vmFactory)
                            CreateGroupScreen(
                                onBack = { mainNavController.popBackStack() },
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

                        composable(AppRoutes.JoinGroup) {
                            val groupListViewModel: GroupListViewModel = viewModel(factory = vmFactory)
                            JoinGroupScreen(
                                onBack = { mainNavController.popBackStack() },
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

                        composable(
                            route = AppRoutes.GroupChat,
                            arguments = listOf(navArgument(AppRoutes.ChatIdArg) { type = NavType.StringType }),
                        ) { backStackEntry ->
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
                                onInviteClick = { groupOnInviteClick = it },
                                onPeersClick = { groupOnPeersClick = it },
                                onLeaveClick = { groupOnLeaveClick = it },
                                onGroupInfoChanged = { name, topic, peerCount, status ->
                                    activeGroupName = name
                                    activeGroupTopic = topic
                                    activeGroupPeerCount = peerCount
                                    activeGroupConnectionStatus = status
                                }
                            )
                        }

                        composable(AppRoutes.AddContactTab) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = LocalTabPadding.current.calculateBottomPadding())
                            ) {
                                AddContactScreen(
                                    viewModel = addContactViewModel,
                                    showBackButton = false,
                                    onSuccess = {
                                        mainNavController.navigate(AppRoutes.Chats) {
                                            launchSingleTop = true
                                            popUpTo(AppRoutes.Chats)
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoutes.Profile) {
                            val user by userState
                            val avatar by profileViewModel.avatar.collectAsStateWithLifecycle()
                            val cropState by profileViewModel.cropState.collectAsStateWithLifecycle()

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = LocalTabPadding.current.calculateBottomPadding())
                            ) {
                                UserProfileScreen(
                                    user = user,
                                    toxId = profileViewModel.toxId.string(),
                                    avatar = avatar,
                                    cropState = cropState,
                                    showBackButton = false,
                                    onSetName = profileViewModel::setName,
                                    onSetStatusMessage = profileViewModel::setStatusMessage,
                                    onSetStatus = profileViewModel::setStatus,
                                    onLogout = {
                                        contactListViewModel.deleteProfileAndData()
                                        navController.navigate(AppRoutes.Launch) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onAvatarChanged = profileViewModel::broadcastAvatar,
                                    onResetCropState = profileViewModel::resetCropState,
                                    onCropAndSaveAvatar = { originalBitmap, scale, offsetX, offsetY, rotation, viewportWidth ->
                                        profileViewModel.cropAndSaveAvatar(originalBitmap, scale, offsetX, offsetY, rotation, viewportWidth)
                                    }
                                )
                            }
                        }

                        composable(AppRoutes.Settings) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = LocalTabPadding.current.calculateBottomPadding())
                            ) {
                                SettingsScreen(
                                    settings = settings,
                                    appearance = appearance,
                                    onThemeChanged = onThemeChanged,
                                    onDynamicColorChanged = onDynamicColorChanged,
                                    onAccentColorSeedChanged = onAccentColorSeedChanged,
                                    onLocaleTagChanged = onLocaleTagChanged,
                                    onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                                    showBackButton = false,
                                    vmFactory = vmFactory,
                                    onTitleChanged = { settingsTitle = it },
                                    onBackActionChanged = { settingsOnBackAction = it },
                                    onSearchActionChanged = { settingsOnSearchAction = it }
                                )
                            }
                        }

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
                }
            }

            composable(AppRoutes.CreateProfile) {
                val viewModel: CreateProfileViewModel = viewModel(factory = vmFactory)
                CreateProfileScreen(
                    viewModel = viewModel,
                    onSuccess = {
                        navController.navigate(AppRoutes.ContactList) {
                            popUpTo(AppRoutes.CreateProfile) { inclusive = true }
                        }
                    }
                )
            }



            composable(
                route = AppRoutes.Call,
                arguments = listOf(navArgument(AppRoutes.PublicKeyArg) { type = NavType.StringType }),
                enterTransition = { AToxMotion.slideUpEnter() },
                exitTransition = { AToxMotion.slideDownExit() },
                popEnterTransition = { AToxMotion.slideUpEnter() },
                popExitTransition = { AToxMotion.slideDownExit() },
            ) { backStackEntry ->
                val publicKeyStr = backStackEntry.arguments?.getString(AppRoutes.PublicKeyArg).orEmpty()
                val viewModel: CallViewModel = viewModel(factory = vmFactory)

                LaunchedEffect(publicKeyStr) {
                    viewModel.setActiveContact(PublicKey(publicKeyStr))
                    callScreenMinimized.value = false
                }

                val contact by viewModel.contact.collectAsStateWithLifecycle()
                val callState by viewModel.inCall.collectAsStateWithLifecycle()
                val sendingAudio by viewModel.sendingAudio.collectAsStateWithLifecycle()
                val speakerphoneOn by viewModel.speakerphoneState.collectAsStateWithLifecycle()
                val callDuration by viewModel.callDuration.collectAsStateWithLifecycle()

                CallScreen(
                    publicKey = publicKeyStr,
                    contact = contact,
                    callState = callState,
                    sendingAudio = sendingAudio,
                    speakerphoneOn = speakerphoneOn,
                    callDuration = callDuration,
                    hasMicPermission = permissionManager.canRecordAudio(),
                    onRequestMicPermission = {},
                    onMinimize = {
                        callScreenMinimized.value = true
                        navController.popBackStack()
                    },
                    onToggleMic = {
                        if (sendingAudio) {
                            viewModel.stopSendingAudio()
                        } else {
                            viewModel.startSendingAudio()
                        }
                    },
                    onToggleSpeaker = viewModel::toggleSpeakerphone,
                    onEndCall = {
                        callScreenMinimized.value = false
                        viewModel.endCall()
                        navController.popBackStack()
                    },
                    hapticEnabled = settings.hapticEnabled,
                )
            }

            composable(
                route = AppRoutes.AddContact,
                arguments = listOf(navArgument(AppRoutes.ToxIdArg) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) { backStackEntry ->
                val toxIdArg = backStackEntry.arguments?.getString(AppRoutes.ToxIdArg).orEmpty()
                val viewModel: AddContactViewModel = viewModel(factory = vmFactory)
                AddContactScreen(
                    viewModel = viewModel,
                    initialToxId = toxIdArg,
                    onBack = navController::popBackStack,
                    onSuccess = {
                        navController.popBackStack()
                    }
                )
            }

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
                val context = LocalContext.current

                ForwardSelectionScreen(
                    contacts = contactsState,
                    settings = settings,
                    onBack = { navController.popBackStack() },
                    onContactSelect = { contact ->
                        contactListViewModel.onShareText(messageText, contact)
                        Toast.makeText(context, context.getString(R.string.message_forwarded), Toast.LENGTH_SHORT).show()
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
                val context = LocalContext.current

                ForwardSelectionScreen(
                    contacts = contactsState,
                    settings = settings,
                    onBack = {
                        MainActivity.sharedContentState.value = null
                        navController.popBackStack()
                    },
                    onContactSelect = { contact ->
                        val content = MainActivity.sharedContentState.value
                        if (content != null) {
                            when (content) {
                                is SharedContent.Text -> {
                                    contactListViewModel.onShareText(content.text, contact)
                                    Toast.makeText(context, context.getString(R.string.message_forwarded), Toast.LENGTH_SHORT).show()
                                }
                                is SharedContent.File -> {
                                    contactListViewModel.onShareFile(content.uri, contact)
                                    Toast.makeText(context, context.getString(R.string.file_sharing_started), Toast.LENGTH_SHORT).show()
                                }
                                is SharedContent.MultipleFiles -> {
                                    content.uris.forEach { uri ->
                                        contactListViewModel.onShareFile(uri, contact)
                                    }
                                    Toast.makeText(context, context.getString(R.string.file_sharing_started), Toast.LENGTH_SHORT).show()
                                }
                            }
                            MainActivity.sharedContentState.value = null
                        }
                        navController.popBackStack()
                        mainNavController.navigate(AppRoutes.chat(contact.publicKey)) {
                            popUpTo(AppRoutes.Chats) { inclusive = false }
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
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
            AlertDialog(
                onDismissRequest = {},
                title = { Text(androidx.compose.ui.res.stringResource(R.string.incoming_call)) },
                text = {
                    Text(
                        androidx.compose.ui.res.stringResource(
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
                        Text(androidx.compose.ui.res.stringResource(R.string.accept))
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
                        Text(androidx.compose.ui.res.stringResource(R.string.reject))
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
