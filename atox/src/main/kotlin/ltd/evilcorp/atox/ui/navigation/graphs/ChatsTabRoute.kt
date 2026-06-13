package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.contactlist.ChatsRouteScreen
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.ui.navigation.LocalAnimatedVisibilityScope
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.core.model.PublicKey

import ltd.evilcorp.atox.ui.theme.AToxMotion

fun NavGraphBuilder.chatsTabRoute(
    navController: NavHostController,
    contactListViewModel: ContactListViewModel,
    settings: Settings,
    isExpanded: () -> Boolean
) {
    composable<AppRoutes.Chats>(
        enterTransition = { AToxMotion.fadeThroughEnter() },
        exitTransition = { AToxMotion.fadeThroughExit() }
    ) {
        val coroutineScope = rememberCoroutineScope()
        val groupListViewModel: GroupListViewModel = hiltViewModel()

        val searchQuery by contactListViewModel.searchQuery.collectAsStateWithLifecycle()
        val friendRequestsViewModel: ltd.evilcorp.atox.ui.friendrequest.FriendRequestsViewModel = hiltViewModel()

        val contacts by contactListViewModel.contacts.collectAsStateWithLifecycle()
        val groupsState = groupListViewModel.groups.collectAsStateWithLifecycle()
        val connectionStatusesState = groupListViewModel.connectionStatuses.collectAsStateWithLifecycle()
        val filteredContacts by contactListViewModel.filteredContacts.collectAsStateWithLifecycle()
        val friendRequests by friendRequestsViewModel.friendRequests.collectAsStateWithLifecycle(emptyList())
        val groupInvite by contactListViewModel.groupInvite.collectAsStateWithLifecycle()
        val groupInviteFriendName by contactListViewModel.groupInviteFriendName.collectAsStateWithLifecycle()
        val user by contactListViewModel.user.collectAsStateWithLifecycle()

        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                ChatsRouteScreen(
                    connectionStatus = user?.connectionStatus ?: ConnectionStatus.None,
                    contacts = contacts,
                    groupsState = groupsState,
                    connectionStatusesState = connectionStatusesState,
                    friendRequests = friendRequests,
                    groupInvite = groupInvite,
                    groupInviteFriendName = groupInviteFriendName,
                    dateFormatPreference = settings.dateFormatPreference,
                    timeFormatPreference = settings.timeFormatPreference,
                    onContactClick = { contact ->
                        contactListViewModel.prepareOpenChat(contact)
                        if (!isExpanded()) {
                            navController.navigate(AppRoutes.Chat(contact.publicKey))
                        }
                    },
                    onGroupClick = { group ->
                        if (isExpanded()) {
                            contactListViewModel.prepareOpenGroup(group)
                        } else {
                            navController.navigate(AppRoutes.GroupChat(group.chatId))
                        }
                    },
                    onLeaveGroup = { group ->
                        coroutineScope.launch {
                            groupListViewModel.leaveGroup(group)
                        }
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
                    onSearchClick = {
                        navController.navigate(AppRoutes.SearchContacts)
                    }
                )
            }
        }
    }
}
