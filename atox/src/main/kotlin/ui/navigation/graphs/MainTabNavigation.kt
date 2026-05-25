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
import androidx.compose.runtime.Composable
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
import androidx.navigation.compose.composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.settings.Settings
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
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.domain.feature.GroupInvite

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.mainTabGraph(
    mainNavController: NavHostController,
    navController: NavHostController,
    vmFactory: ViewModelProvider.Factory,
    contactListViewModel: ContactListViewModel,
    settings: Settings,
    appearance: AppAppearance,
    isSearchingState: MutableState<Boolean>,
    settingsTitleState: MutableState<String>,
    settingsOnBackActionState: MutableState<(() -> Unit)?>,
    settingsOnSearchActionState: MutableState<(() -> Unit)?>,
    contactsState: State<List<Contact>>,
    friendRequestsState: State<List<FriendRequest>>,
    groupInviteState: State<GroupInvite?>,
    groupInviteFriendNameState: State<String>,
    coroutineScope: CoroutineScope,
    onThemeChanged: (Int) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onAccentColorSeedChanged: (Int) -> Unit,
    onLocaleTagChanged: (String) -> Unit,
    onDisableScreenshotsChanged: (Boolean) -> Unit,
) {
    composable(route = AppRoutes.Chats) {
        val context = LocalContext.current
        val isSearchingLocal = isSearchingState.value
        val searchQuery by contactListViewModel.searchQuery.collectAsStateWithLifecycle()
        val friendRequestsViewModel: ltd.evilcorp.atox.ui.friendrequest.FriendRequestsViewModel = viewModel(factory = vmFactory)

        LaunchedEffect(Unit) {
            AppBarStateHolder.config.value = null
        }

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
                isSearching = isSearchingState.value,
                onSearchingChanged = { isSearchingState.value = it }
            )
        }
    }

    composable(route = AppRoutes.Groups) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            AppBarStateHolder.config.value = AppBarConfig(
                title = {
                    Text(
                        text = context.getString(R.string.groups),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        val groupListViewModel: GroupListViewModel = viewModel(factory = vmFactory)
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
                    mainNavController.navigate(AppRoutes.groupChat(group.chatId))
                },
                onCreateGroupClick = {
                    mainNavController.navigate(AppRoutes.CreateGroup)
                },
                onJoinGroupClick = {
                    mainNavController.navigate(AppRoutes.JoinGroup)
                },
                onLeaveGroup = { group ->
                    coroutineScope.launch {
                        groupListViewModel.leaveGroup(group)
                    }
                }
            )
        }
    }

    composable(AppRoutes.AddContactTab) {
        val context = LocalContext.current
        val addContactViewModel: AddContactViewModel = viewModel(factory = vmFactory)

        LaunchedEffect(Unit) {
            AppBarStateHolder.config.value = AppBarConfig(
                title = {
                    Text(
                        text = context.getString(R.string.add_contact),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
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
                    mainNavController.navigate(AppRoutes.Chats) {
                        launchSingleTop = true
                        popUpTo(AppRoutes.Chats)
                    }
                }
            )
        }
    }

    composable(AppRoutes.Profile) {
        val context = LocalContext.current
        val profileViewModel: UserProfileViewModel = viewModel(factory = vmFactory)

        LaunchedEffect(Unit) {
            AppBarStateHolder.config.value = AppBarConfig(
                title = {
                    Text(
                        text = context.getString(R.string.profile),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
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

    composable(AppRoutes.Settings) {
        val context = LocalContext.current
        val settingsTitle = settingsTitleState.value
        val settingsOnBackAction = settingsOnBackActionState.value
        val settingsOnSearchAction = settingsOnSearchActionState.value

        LaunchedEffect(settingsTitle, settingsOnBackAction, settingsOnSearchAction) {
            AppBarStateHolder.config.value = AppBarConfig(
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
                                contentDescription = "Back"
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
                vmFactory = vmFactory,
                onTitleChanged = { settingsTitleState.value = it },
                onBackActionChanged = { settingsOnBackActionState.value = it },
                onSearchActionChanged = { settingsOnSearchActionState.value = it }
            )
        }
    }
}
