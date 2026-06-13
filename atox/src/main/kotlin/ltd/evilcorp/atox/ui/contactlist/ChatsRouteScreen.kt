package ltd.evilcorp.atox.ui.contactlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyListState
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.AtoxConfirmDialog
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.atox.ui.contactlist.components.ChatListTab
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import androidx.compose.runtime.State
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import ltd.evilcorp.domain.features.group.GroupInvite
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color

import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

@Suppress("FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsRouteScreen(
    connectionStatus: ConnectionStatus,
    contacts: List<Contact>,
    groupsState: State<List<Group>>,
    connectionStatusesState: State<Map<String, GroupConnectionStatus>>,
    friendRequests: List<FriendRequest>,
    groupInvite: GroupInvite?,
    groupInviteFriendName: String,
    dateFormatPreference: DateFormatPreference,
    timeFormatPreference: TimeFormatPreference,
    onContactClick: (Contact) -> Unit,
    onGroupClick: (Group) -> Unit,
    onLeaveGroup: (Group) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onAcceptFriendRequest: (FriendRequest) -> Unit,
    onRejectFriendRequest: (FriendRequest) -> Unit,
    onAcceptGroupInvite: () -> Unit,
    onRejectGroupInvite: () -> Unit,
    onAddContactClick: () -> Unit,
    onContactInteraction: () -> Unit,
    onSearchClick: () -> Unit,
) {
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }
    var groupToLeave by remember { mutableStateOf<Group?>(null) }
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }

    val transitionAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0.85f else 0.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "topBarAlpha"
    )
    val transitionElevation by animateDpAsState(
        targetValue = if (isScrolled) 4.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "topBarElevation"
    )

    val appNameString = stringResource(R.string.app_name)
    val connectingString = stringResource(R.string.connecting)

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = transitionElevation,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = transitionAlpha),
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = appNameString,
                                fontWeight = FontWeight.Bold
                            )
                            if (connectionStatus == ConnectionStatus.None) {
                                Text(
                                    text = connectingString,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                ChatListTab(
                    contacts = contacts,
                    groups = groupsState.value,
                    connectionStatuses = connectionStatusesState.value,
                    friendRequests = friendRequests,
                    groupInvite = groupInvite,
                    groupInviteFriendName = groupInviteFriendName,
                    listState = listState,
                    dateFormatPreference = dateFormatPreference,
                    timeFormatPreference = timeFormatPreference,
                    onContactClick = onContactClick,
                    onGroupClick = onGroupClick,
                    onLeaveGroup = { groupToLeave = it },
                    onDeleteContact = { contactToDelete = it },
                    onAcceptFriendRequest = onAcceptFriendRequest,
                    onRejectFriendRequest = onRejectFriendRequest,
                    onAcceptGroupInvite = onAcceptGroupInvite,
                    onRejectGroupInvite = onRejectGroupInvite,
                    onAddContactClick = onAddContactClick,
                    onContactInteraction = onContactInteraction,
                )
            }
        }

        contactToDelete?.let { contact ->
            AtoxConfirmDialog(
                onDismiss = { contactToDelete = null },
                onConfirm = {
                    contactToDelete = null
                    onDeleteContact(contact)
                },
                title = stringResource(R.string.contact_list_delete_contact),
                text = stringResource(R.string.contact_list_delete_contact_confirm, contact.name.ifEmpty { stringResource(R.string.contact_default_name) }),
                confirmText = stringResource(R.string.delete),
                dismissText = stringResource(android.R.string.cancel),
                isDangerous = true,
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            )
        }

        groupToLeave?.let { group ->
            AtoxConfirmDialog(
                onDismiss = { groupToLeave = null },
                onConfirm = {
                    groupToLeave = null
                    onLeaveGroup(group)
                },
                title = stringResource(R.string.group_leave),
                text = stringResource(R.string.group_leave_confirm, group.name),
                confirmText = stringResource(R.string.confirm),
                dismissText = stringResource(android.R.string.cancel),
                isDangerous = true
            )
        }
}
}
