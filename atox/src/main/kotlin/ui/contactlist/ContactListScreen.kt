package ltd.evilcorp.atox.ui.contactlist

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.ui.settings.SettingsScreen
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.atox.ui.common.PresenceTone
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.atox.ui.common.formatPresenceText
import ltd.evilcorp.atox.ui.theme.*
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.model.User
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.atox.ui.userprofile.UserProfileScreen
import ltd.evilcorp.atox.ui.addcontact.AddContactScreen
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.atox.ui.groupchat.GroupListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    userState: State<User?>,
    contactsState: State<List<Contact>>,
    friendRequestsState: State<List<FriendRequest>>,
    groupsState: State<List<Group>>,
    groupConnectionStatusesState: State<Map<String, GroupConnectionStatus>>,
    onAddContact: (String, String) -> Unit,
    onContactClick: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onAcceptFriendRequest: (FriendRequest) -> Unit,
    onRejectFriendRequest: (FriendRequest) -> Unit,
    onGroupClick: (Group) -> Unit,
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    onLeaveGroup: (Group) -> Unit,
    onQuitTox: () -> Unit,
    
    // Пропсы для профиля и настроек в таб-баре
    toxId: String,
    onSetName: (String) -> Unit,
    onSetStatusMessage: (String) -> Unit,
    onSetStatus: (UserStatus) -> Unit,
    settings: Settings,
    appearance: AppAppearance,
    onThemeChanged: (Int) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onAccentColorSeedChanged: (Int) -> Unit,
    onLocaleTagChanged: (String) -> Unit,
    onDisableScreenshotsChanged: (Boolean) -> Unit,
    onLogout: () -> Unit = {},
    onAvatarChanged: () -> Unit = {},
    vmFactory: ViewModelProvider.Factory? = null
) {
    val haptic = LocalHapticFeedback.current
    val performHaptic = {
        if (settings.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val filteredContacts = remember(contactsState.value, searchQuery) {
        if (searchQuery.isEmpty()) {
            contactsState.value
        } else {
            contactsState.value.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.statusMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (selectedTab == 0) {
                if (isSearching) {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onSearch = {},
                                expanded = true,
                                onExpandedChange = { if (!it) isSearching = false },
                                placeholder = { Text(stringResource(R.string.contact_list_search_placeholder)) },
                                leadingIcon = {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        isSearching = false
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                        },
                        expanded = true,
                        onExpandedChange = { if (!it) isSearching = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {}
                } else {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {},
                        actions = {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        performHaptic()
                        selectedTab = 0
                    },
                    icon = { Icon(Icons.Default.Email, contentDescription = "Chats") },
                    label = { Text(stringResource(R.string.chats)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        performHaptic()
                        selectedTab = 1
                    },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Groups") },
                    label = { Text(stringResource(R.string.groups)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        performHaptic()
                        selectedTab = 2
                    },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact") },
                    label = { Text(stringResource(R.string.add_contact_tab)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        performHaptic()
                        selectedTab = 3
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text(stringResource(R.string.profile)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        performHaptic()
                        selectedTab = 4
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(stringResource(R.string.settings)) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    val requests = friendRequestsState.value
                    val isListEmpty = requests.isEmpty() && filteredContacts.isEmpty()

                    if (isListEmpty) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.no_contacts_call_to_action),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            if (requests.isNotEmpty()) {
                                items(requests) { req ->
                                    FriendRequestItemCard(
                                        request = req,
                                        onAccept = { onAcceptFriendRequest(req) },
                                        onReject = { onRejectFriendRequest(req) }
                                    )
                                }
                            }

                            items(filteredContacts) { contact ->
                                ContactItemCard(
                                    contact = contact,
                                    settings = settings,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onContactClick(contact)
                                    },
                                    onDelete = { onDeleteContact(contact) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    GroupListScreen(
                        groupsState = groupsState,
                        connectionStatusesState = groupConnectionStatusesState,
                        onGroupClick = { group ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGroupClick(group)
                        },
                        onCreateGroupClick = onCreateGroupClick,
                        onJoinGroupClick = onJoinGroupClick,
                        onLeaveGroup = onLeaveGroup
                    )
                }
                2 -> {
                    AddContactScreen(
                        showBackButton = false,
                        onAddContact = { toxIdStr, message ->
                            onAddContact(toxIdStr, message)
                            selectedTab = 0
                        }
                    )
                }
                3 -> {
                    UserProfileScreen(
                        userState = userState,
                        toxId = toxId,
                        showBackButton = false,
                        onSetName = onSetName,
                        onSetStatusMessage = onSetStatusMessage,
                        onSetStatus = onSetStatus,
                        onLogout = onLogout,
                        onAvatarChanged = onAvatarChanged
                    )
                }
                4 -> {
                    SettingsScreen(
                        settings = settings,
                        appearance = appearance,
                        onThemeChanged = onThemeChanged,
                        onDynamicColorChanged = onDynamicColorChanged,
                        onAccentColorSeedChanged = onAccentColorSeedChanged,
                        onLocaleTagChanged = onLocaleTagChanged,
                        onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                        showBackButton = false,
                        vmFactory = vmFactory
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItemCard(
    contact: Contact,
    settings: Settings,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val presence = remember(contact, settings.dateFormatPreference, settings.timeFormatPreference) {
        formatPresenceText(
            context = context,
            contact = contact,
            dateFormatPreference = settings.dateFormatPreference,
            timeFormatPreference = settings.timeFormatPreference,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            )
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Avatar Box without clipping parent
            Box(
                modifier = Modifier.size(48.dp)
            ) {
                ContactAvatar(
                    name = contact.name,
                    publicKey = contact.publicKey,
                    avatarUri = contact.avatarUri,
                    size = 48.dp,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxSize()
                )

                // Online/Offline Status Indicator overlay without clip
                val statusColor = when (contact.connectionStatus) {
                    ConnectionStatus.None -> StatusOffline
                    ConnectionStatus.TCP, ConnectionStatus.UDP -> when (contact.status) {
                        ltd.evilcorp.core.model.UserStatus.None -> StatusAvailable
                        ltd.evilcorp.core.model.UserStatus.Away -> StatusAway
                        ltd.evilcorp.core.model.UserStatus.Busy -> StatusBusy
                    }
                }

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info Stack (Name & Last Message)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name.ifEmpty { stringResource(R.string.contact_default_name) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Beautiful micro-animation for typing state!
                if (contact.typing) {
                    val infiniteTransition = rememberInfiniteTransition(label = "typing")
                    val alphaAnimation by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "typingAlpha"
                    )

                    Text(
                        text = stringResource(R.string.contact_typing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.graphicsLayer(alpha = alphaAnimation)
                    )
                } else if (contact.draftMessage.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.draft_message, contact.draftMessage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = presence.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (presence.color) {
                            PresenceTone.Online -> StatusAvailable
                            PresenceTone.Away -> StatusAway
                            PresenceTone.Busy -> StatusBusy
                            PresenceTone.Accent -> MaterialTheme.colorScheme.primary
                            PresenceTone.Muted -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Date & Unread count badge stack
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val dateText = remember(contact.lastMessage, settings.timeFormatPreference) {
                    if (contact.lastMessage != 0L) {
                        formatChatTime(context, contact.lastMessage, settings.timeFormatPreference)
                    } else {
                        ""
                    }
                }

                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (contact.hasUnreadMessages) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }
            }
        }

        // Beautiful flat divider offset by avatar width
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 76.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete)
                    )
                },
                title = { Text(stringResource(R.string.contact_list_delete_contact)) },
                text = {
                    Text(
                        stringResource(
                            R.string.contact_list_delete_contact_confirm,
                            contact.name.ifEmpty { stringResource(R.string.contact_default_name) }
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete()
                        }
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun FriendRequestItemCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.contact_list_friend_request),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = request.publicKey,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = request.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.accept))
                }
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.reject))
                }
            }
        }
    }
}
