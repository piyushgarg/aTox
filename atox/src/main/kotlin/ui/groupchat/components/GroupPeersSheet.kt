// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.groupchat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.GroupPeer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPeersSheet(
    onDismissRequest: () -> Unit,
    peers: List<GroupPeer>,
    contacts: List<Contact>,
    selfAvatarUri: String
) {
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
                text = stringResource(R.string.group_peers),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
            ) {
                items(peers) { peer ->
                    val matchingContact = contacts.find { it.publicKey.equals(peer.publicKey, ignoreCase = true) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        ListItem(
                            leadingContent = {
                                ContactAvatar(
                                    name = peer.name.ifEmpty { "Unknown" },
                                    publicKey = peer.publicKey,
                                    avatarUri = if (peer.isOurselves) selfAvatarUri else (matchingContact?.avatarUri ?: ""),
                                    size = 40.dp,
                                    fontSize = 15.sp
                                )
                            },
                            headlineContent = {
                                Text(
                                    text = peer.name.ifEmpty { stringResource(R.string.contact_default_name) },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (peer.isOurselves) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(
                                        when (peer.role) {
                                            "Owner" -> R.string.group_role_owner
                                            "Moderator" -> R.string.group_role_moderator
                                            "Observer" -> R.string.group_role_observer
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
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
