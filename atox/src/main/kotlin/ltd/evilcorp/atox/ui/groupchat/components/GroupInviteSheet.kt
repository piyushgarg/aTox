// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.groupchat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.GroupPeer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInviteSheet(
    onDismissRequest: () -> Unit,
    onCopyInvite: () -> Unit,
    onInviteFriend: (friendPublicKey: String) -> Unit,
    peers: List<GroupPeer>,
    contacts: List<Contact>,
    settings: Settings,
    onInviteResult: (String) -> Unit
) {
    val context = LocalContext.current
    var inviteSearchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isCopying by remember { mutableStateOf(false) }
    var invitingContactId by remember { mutableStateOf<String?>(null) }

    val inviteCopiedText = stringResource(R.string.group_invite_copied)
    val inviteSentText = stringResource(R.string.group_invite_sent)
    val inviteFailedText = stringResource(R.string.group_invite_failed)

    val performHaptic = {
        if (settings.hapticEnabled) {
            // Can perform haptic internally if needed, but not strictly required
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.group_invite),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isCopying) {
                        isCopying = true
                        scope.launch {
                            onCopyInvite()
                            isCopying = false
                            onDismissRequest()
                            Toast.makeText(context, inviteCopiedText, Toast.LENGTH_SHORT).show()
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCopying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.0.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (isCopying) "Generating link..." else stringResource(R.string.group_copy_invite),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isCopying) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inviteSearchQuery,
                onValueChange = { inviteSearchQuery = it },
                placeholder = { Text(stringResource(R.string.contact_list_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (inviteSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { inviteSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Text(
                text = stringResource(R.string.group_invite_select_friend),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val filteredContacts = remember(contacts, inviteSearchQuery) {
                if (inviteSearchQuery.isEmpty()) {
                    contacts
                } else {
                    contacts.filter { contact ->
                        contact.name.contains(inviteSearchQuery, ignoreCase = true) ||
                        contact.publicKey.contains(inviteSearchQuery, ignoreCase = true)
                    }
                }
            }

            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (inviteSearchQuery.isEmpty()) "No friends to invite" else "No matching friends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    items(filteredContacts) { contact ->
                        val alreadyInGroup = peers.any { it.publicKey == contact.publicKey }
                        val isThisContactInviting = invitingContactId == contact.publicKey
                        val anyActionPending = isCopying || invitingContactId != null

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = !alreadyInGroup && !anyActionPending,
                                        onClick = {
                                            invitingContactId = contact.publicKey

                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    onInviteFriend(contact.publicKey)
                                                    withContext(Dispatchers.Main) {
                                                        onDismissRequest()
                                                        onInviteResult(inviteSentText)
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        onInviteResult(inviteFailedText)
                                                    }
                                                } finally {
                                                    withContext(Dispatchers.Main) {
                                                        invitingContactId = null
                                                    }
                                                }
                                            }
                                        }
                                    ),
                                leadingContent = {
                                    if (isThisContactInviting) {
                                        Box(
                                            modifier = Modifier.size(40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.fillMaxSize(),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        ContactAvatar(
                                            name = contact.name,
                                            publicKey = contact.publicKey,
                                            avatarUri = contact.avatarUri,
                                            size = 40.dp,
                                            fontSize = 15.sp
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        text = contact.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (anyActionPending && !isThisContactInviting) {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                supportingContent = {
                                    if (alreadyInGroup) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.group_already_in_group),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isThisContactInviting) {
                                        Text(
                                            text = "Sending invitation...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (contact.connectionStatus != ltd.evilcorp.domain.model.ConnectionStatus.None) {
                                        Text(
                                            text = stringResource(R.string.chat_status_online),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
