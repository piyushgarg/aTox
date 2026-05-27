package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import ltd.evilcorp.atox.ui.navigation.LocalAnimatedVisibilityScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.addcontact.AddContactScreen
import ltd.evilcorp.atox.ui.addcontact.AddContactViewModel
import ltd.evilcorp.atox.ui.common.MorphingNavigationIcon
import ltd.evilcorp.atox.ui.contactlist.ChatsRouteScreen
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupListScreen
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.navigation.AppBarConfig
import ltd.evilcorp.atox.ui.navigation.AppBarStateHolder
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.navigation.LocalTabPadding
import ltd.evilcorp.atox.ui.settings.SettingsScreen
import ltd.evilcorp.atox.ui.userprofile.UserProfileScreen
import ltd.evilcorp.atox.ui.userprofile.UserProfileViewModel
import ltd.evilcorp.domain.model.PublicKey

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.mainTabGraph(
    navController: NavHostController,
    contactListViewModel: ContactListViewModel,
    settings: Settings,
    appearance: AppAppearance,
    onThemeChanged: (Int) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onAccentColorSeedChanged: (Int) -> Unit,
    onLocaleTagChanged: (String) -> Unit,
    onDisableScreenshotsChanged: (Boolean) -> Unit,
) {
    composable<AppRoutes.Chats> {
        val searchQuery by contactListViewModel.searchQuery.collectAsStateWithLifecycle()
        val friendRequestsViewModel: ltd.evilcorp.atox.ui.friendrequest.FriendRequestsViewModel = hiltViewModel()

        val contacts by contactListViewModel.contacts.collectAsStateWithLifecycle()
        val friendRequests by friendRequestsViewModel.friendRequests.collectAsStateWithLifecycle(emptyList())
        val groupInvite by contactListViewModel.groupInvite.collectAsStateWithLifecycle()
        val groupInviteFriendName by contactListViewModel.groupInviteFriendName.collectAsStateWithLifecycle()

        val isSearchingState = rememberSaveable { mutableStateOf(false) }

        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = LocalTabPadding.current.calculateBottomPadding())
            ) {
                ChatsRouteScreen(
                    contacts = contacts,
                    friendRequests = friendRequests,
                    groupInvite = groupInvite,
                    groupInviteFriendName = groupInviteFriendName,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = contactListViewModel::setSearchQuery,
                    dateFormatPreference = settings.dateFormatPreference,
                    timeFormatPreference = settings.timeFormatPreference,
                    onContactClick = { contact ->
                        contactListViewModel.prepareOpenChat(contact)
                        navController.navigate(AppRoutes.Chat(contact.publicKey))
                    },
                    onDeleteContact = { contact -> contactListViewModel.deleteContact(PublicKey(contact.publicKey)) },
                    onAcceptFriendRequest = { req -> friendRequestsViewModel.acceptFriendRequest(req) },
                    onRejectFriendRequest = { req -> friendRequestsViewModel.rejectFriendRequest(req) },
                    onAcceptGroupInvite = contactListViewModel::acceptGroupInvite,
                    onRejectGroupInvite = contactListViewModel::declineGroupInvite,
                    onAddContactClick = {
                        navController.navigate(AppRoutes.AddContactTab) {
                            launchSingleTop = true
                        }
                    },
                    onContactInteraction = {},
                    isSearching = isSearchingState.value,
                    onSearchingChanged = { isSearchingState.value = it }
                )
            }
        }
    }

    composable<AppRoutes.Groups> {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val groupsRouteName = AppRoutes.Groups::class.qualifiedName!!

        LaunchedEffect(Unit) {
            AppBarStateHolder.register(
                route = groupsRouteName,
                cfg = AppBarConfig(
                    title = {
                        Text(
                            text = context.getString(R.string.groups),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                AppBarStateHolder.unregister(groupsRouteName)
            }
        }

        val groupListViewModel: GroupListViewModel = hiltViewModel()
        val groupsState = groupListViewModel.groups.collectAsStateWithLifecycle()
        val connectionStatusesState = groupListViewModel.connectionStatuses.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = LocalTabPadding.current.calculateBottomPadding())
        ) {
            GroupListScreen(
                groupsState = groupsState,
                connectionStatusesState = connectionStatusesState,
                onGroupClick = { group ->
                    navController.navigate(AppRoutes.GroupChat(group.chatId))
                },
                onCreateGroupClick = {
                    navController.navigate(AppRoutes.CreateGroup)
                },
                onJoinGroupClick = {
                    navController.navigate(AppRoutes.JoinGroup)
                },
                onLeaveGroup = { group ->
                    coroutineScope.launch {
                        groupListViewModel.leaveGroup(group)
                    }
                }
            )
        }
    }

    composable<AppRoutes.AddContactTab> {
        val context = LocalContext.current
        val addContactViewModel: AddContactViewModel = hiltViewModel()
        val addContactTabRouteName = AppRoutes.AddContactTab::class.qualifiedName!!

        LaunchedEffect(Unit) {
            AppBarStateHolder.register(
                route = addContactTabRouteName,
                cfg = AppBarConfig(
                    title = {
                        Text(
                            text = context.getString(R.string.add_contact),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(R.string.navigation_back)
                            )
                        }
                    }
                )
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                AppBarStateHolder.unregister(addContactTabRouteName)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = LocalTabPadding.current.calculateBottomPadding())
        ) {
            AddContactScreen(
                viewModel = addContactViewModel,
                showBackButton = false,
                onSuccess = {
                    navController.navigate(AppRoutes.Chats) {
                        launchSingleTop = true
                        popUpTo(AppRoutes.Chats)
                    }
                }
            )
        }
    }

    composable<AppRoutes.Profile> {
        val context = LocalContext.current
        val profileViewModel: UserProfileViewModel = hiltViewModel()
        val profileRouteName = AppRoutes.Profile::class.qualifiedName!!

        LaunchedEffect(Unit) {
            AppBarStateHolder.register(
                route = profileRouteName,
                cfg = AppBarConfig(
                    title = {
                        Text(
                            text = context.getString(R.string.profile),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                AppBarStateHolder.unregister(profileRouteName)
            }
        }

        val user by contactListViewModel.user.collectAsStateWithLifecycle()
        val avatar by profileViewModel.avatar.collectAsStateWithLifecycle()
        val cropState by profileViewModel.cropState.collectAsStateWithLifecycle()
        val storedSettings by settings.state.collectAsStateWithLifecycle()
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val performHaptic = {
            if (storedSettings.hapticEnabled) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            }
        }

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
                performHaptic = performHaptic,
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

    composable<AppRoutes.Settings> {
        val context = LocalContext.current
        val settingsTitleState = remember { mutableStateOf("") }
        val settingsOnBackActionState = remember { mutableStateOf<(() -> Unit)?>(null) }
        val settingsOnSearchActionState = remember { mutableStateOf<(() -> Unit)?>(null) }

        val settingsTitle = settingsTitleState.value
        val settingsOnBackAction = settingsOnBackActionState.value
        val settingsOnSearchAction = settingsOnSearchActionState.value
        val settingsRouteName = AppRoutes.Settings::class.qualifiedName!!

        LaunchedEffect(settingsTitle, settingsOnBackAction, settingsOnSearchAction) {
            if (settingsTitle == context.getString(R.string.search_settings)) {
                AppBarStateHolder.unregister(settingsRouteName)
            } else {
                AppBarStateHolder.register(
                    route = settingsRouteName,
                    cfg = AppBarConfig(
                        title = {
                            Text(
                                text = if (settingsOnBackAction == null) context.getString(R.string.settings) else settingsTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            if (settingsOnBackAction != null) {
                                IconButton(onClick = { settingsOnBackAction.invoke() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = context.getString(R.string.navigation_back)
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.padding(start = 4.dp)) {
                                    MorphingNavigationIcon(
                                        isBack = false,
                                        onClick = { settingsOnSearchAction?.invoke() }
                                    )
                                }
                            }
                        }
                    )
                )
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                AppBarStateHolder.unregister(settingsRouteName)
            }
        }

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

                onTitleChanged = { settingsTitleState.value = it },
                onBackActionChanged = { settingsOnBackActionState.value = it },
                onSearchActionChanged = { settingsOnSearchActionState.value = it }
            )
        }
    }
}
