// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.groupchat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.group.model.GroupPeer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPeersSheet(
    onDismissRequest: () -> Unit,
    peers: List<GroupPeer>,
    contacts: List<Contact>,
    selfAvatarUri: String
) {
    val sortedPeers = remember(peers) {
        peers.sortedWith(
            compareByDescending<GroupPeer> { it.role == "FOUNDER" || it.role == "Owner" }
                .thenByDescending { it.isOurselves }
        )
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
                text = stringResource(R.string.group_info),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
            ) {
                items(sortedPeers) { peer ->
                    val matchingContact = contacts.find { it.publicKey.equals(peer.publicKey, ignoreCase = true) }
                    val isOnline = when {
                        peer.isOurselves -> true
                        matchingContact != null -> matchingContact.connectionStatus != ConnectionStatus.None
                        else -> true
                    }
                    val statusColor = if (isOnline) ltd.evilcorp.atox.ui.theme.StatusAvailable else ltd.evilcorp.atox.ui.theme.StatusOffline

                    val isOwner = peer.role == "FOUNDER" || peer.role == "Owner"
                    val cardBorder = if (isOwner) {
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    } else {
                        null
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = cardBorder,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOwner) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        ListItem(
                            leadingContent = {
                                Box(modifier = Modifier.size(40.dp)) {
                                    ContactAvatar(
                                        name = peer.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                        publicKey = peer.publicKey,
                                        avatarUri = if (peer.isOurselves) selfAvatarUri else (matchingContact?.avatarUri ?: ""),
                                        size = 40.dp,
                                        fontSize = 15.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(statusColor, shape = CircleShape)
                                            .border(1.5.dp, MaterialTheme.colorScheme.surface, shape = CircleShape)
                                            .align(androidx.compose.ui.Alignment.BottomEnd)
                                    )
                                }
                            },
                            headlineContent = {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text(
                                        text = peer.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (peer.isOurselves) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isOwner) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = stringResource(R.string.group_role_owner),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(
                                        when (peer.role) {
                                            "FOUNDER", "Owner" -> R.string.group_role_owner
                                            "MODERATOR", "Moderator" -> R.string.group_role_moderator
                                            "OBSERVER", "Observer" -> R.string.group_role_observer
                                            else -> R.string.group_role_user
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            // Diagnostics Card
            val groupOwner = sortedPeers.find { it.role == "FOUNDER" || it.role == "Owner" }
            val matchingOwnerContact = groupOwner?.let { owner ->
                contacts.find { it.publicKey.equals(owner.publicKey, ignoreCase = true) }
            }
            val isOwnerOnline = when {
                groupOwner == null -> false
                groupOwner.isOurselves -> true
                matchingOwnerContact != null -> matchingOwnerContact.connectionStatus != ConnectionStatus.None
                else -> false
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.group_diagnostics_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.group_diagnostics_host,
                            groupOwner?.name ?: stringResource(R.string.contact_default_name)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    val hostStatusStr = when {
                        groupOwner == null -> stringResource(R.string.group_diagnostics_status_undefined)
                        groupOwner.isOurselves -> stringResource(R.string.group_diagnostics_status_self)
                        isOwnerOnline -> stringResource(R.string.group_diagnostics_status_online)
                        else -> stringResource(R.string.group_diagnostics_status_offline)
                    }
                    Text(
                        text = stringResource(R.string.group_diagnostics_status_label, hostStatusStr),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            groupOwner == null -> stringResource(R.string.group_diagnostics_desc_undefined)
                            groupOwner.isOurselves -> stringResource(R.string.group_diagnostics_desc_self)
                            isOwnerOnline -> stringResource(R.string.group_diagnostics_desc_online)
                            else -> stringResource(R.string.group_diagnostics_desc_offline)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
