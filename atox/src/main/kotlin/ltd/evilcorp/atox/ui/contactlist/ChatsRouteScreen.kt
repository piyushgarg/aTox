package ltd.evilcorp.atox.ui.contactlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.MorphingNavigationIcon
import ltd.evilcorp.atox.ui.common.AtoxAppBar
import ltd.evilcorp.atox.ui.common.AtoxConfirmDialog
import ltd.evilcorp.atox.ui.navigation.AppBarConfig
import ltd.evilcorp.atox.ui.navigation.AppBarStateHolder
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.atox.ui.contactlist.components.ChatListTab
import ltd.evilcorp.atox.ui.contactlist.components.ContactItemCard
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.DateFormatPreference
import ltd.evilcorp.domain.model.FriendRequest
import ltd.evilcorp.domain.model.TimeFormatPreference
import ltd.evilcorp.domain.feature.GroupInvite
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import ltd.evilcorp.atox.ui.common.ContactAvatar
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.remember

@Suppress("FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsRouteScreen(
    connectionStatus: ConnectionStatus,
    contacts: List<Contact>,
    friendRequests: List<FriendRequest>,
    groupInvite: GroupInvite?,
    groupInviteFriendName: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    dateFormatPreference: DateFormatPreference,
    timeFormatPreference: TimeFormatPreference,
    onContactClick: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onAcceptFriendRequest: (FriendRequest) -> Unit,
    onRejectFriendRequest: (FriendRequest) -> Unit,
    onAcceptGroupInvite: () -> Unit,
    onRejectGroupInvite: () -> Unit,
    onAddContactClick: () -> Unit,
    onContactInteraction: () -> Unit,
    isSearching: Boolean = false,
    onSearchingChanged: (Boolean) -> Unit = {},
) {
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    val appNameString = stringResource(R.string.app_name)
    val connectingString = stringResource(R.string.connecting)
    if (!isSearching) {
        AtoxAppBar(
            route = AppRoutes.Chats::class.qualifiedName!!,
            config = AppBarConfig(
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
                navigationIcon = {
                    IconButton(onClick = { onSearchingChanged(true) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                },
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSearching) {
            ltd.evilcorp.atox.ui.common.AtoxSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChanged,
                onSearch = {},
                active = true,
                onActiveChange = onSearchingChanged,
                placeholder = stringResource(R.string.contact_list_search_placeholder),
                modifier = Modifier.fillMaxSize()
            ) {
                    val filteredContacts = remember(searchQuery, contacts) {
                        if (searchQuery.isBlank()) emptyList()
                        else contacts.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.publicKey.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredContacts,
                            key = { contact -> contact.publicKey }
                        ) { contact ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = contact.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = contact.publicKey,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingContent = {
                                    ContactAvatar(
                                        name = contact.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                        publicKey = contact.publicKey,
                                        avatarUri = contact.avatarUri,
                                        size = 40.dp,
                                        fontSize = 16.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSearchingChanged(false)
                                        onSearchQueryChanged("")
                                        onContactClick(contact)
                                    }
                            )
                        }
                    }
                }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ChatListTab(
                contacts = contacts,
                friendRequests = friendRequests,
                groupInvite = groupInvite,
                groupInviteFriendName = groupInviteFriendName,
                listState = listState,
                searchQuery = searchQuery,
                dateFormatPreference = dateFormatPreference,
                timeFormatPreference = timeFormatPreference,
                onContactClick = onContactClick,
                onDeleteContact = { contactToDelete = it },
                onAcceptFriendRequest = onAcceptFriendRequest,
                onRejectFriendRequest = onRejectFriendRequest,
                onAcceptGroupInvite = onAcceptGroupInvite,
                onRejectGroupInvite = onRejectGroupInvite,
                onAddContactClick = onAddContactClick,
                onContactInteraction = onContactInteraction,
            )
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
    }
}
