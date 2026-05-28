package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.groupchat.GroupListScreen
import ltd.evilcorp.atox.ui.groupchat.GroupListViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.navigation.LocalTabPadding
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.groupsTabRoute(
    navController: NavHostController
) {
    composable<AppRoutes.Groups> {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val groupListViewModel: GroupListViewModel = hiltViewModel()
        val user by groupListViewModel.user.collectAsStateWithLifecycle()
        val connectionStatus = user?.connectionStatus ?: ConnectionStatus.None

        val groupsState = groupListViewModel.groups.collectAsStateWithLifecycle()
        val connectionStatusesState = groupListViewModel.connectionStatuses.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = context.getString(R.string.groups),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (connectionStatus == ConnectionStatus.None) {
                                Text(
                                    text = context.getString(R.string.connecting),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
    }
}
