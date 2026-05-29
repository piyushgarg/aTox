package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.contactlist.ChatsRouteScreen
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.navigation.LocalAnimatedVisibilityScope
import ltd.evilcorp.atox.ui.navigation.LocalTabPadding
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.core.model.PublicKey

fun NavGraphBuilder.chatsTabRoute(
    navController: NavHostController,
    contactListViewModel: ContactListViewModel,
    settings: Settings,
    isExpanded: () -> Boolean
) {
    composable<AppRoutes.Chats> {
        val searchQuery by contactListViewModel.searchQuery.collectAsStateWithLifecycle()
        val friendRequestsViewModel: ltd.evilcorp.atox.ui.friendrequest.FriendRequestsViewModel = hiltViewModel()

        val contacts by contactListViewModel.contacts.collectAsStateWithLifecycle()
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
