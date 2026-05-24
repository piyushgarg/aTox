package ltd.evilcorp.atox.ui.groupchat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupPeer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    groupsState: State<List<Group>>,
    connectionStatusesState: State<Map<String, GroupConnectionStatus>>,
    onGroupClick: (Group) -> Unit,
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    onLeaveGroup: (Group) -> Unit,
) {
    val groups = groupsState.value
    val connectionStatuses = connectionStatusesState.value
    val haptic = LocalHapticFeedback.current
    var showLeaveDialog by remember { mutableStateOf<Group?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (groups.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_groups),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_groups_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCreateGroupClick()
                }) {
                    Text(stringResource(R.string.create_group))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onJoinGroupClick()
                }) {
                    Text(stringResource(R.string.join_group))
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(groups) { group ->
                        GroupItemCard(
                            group = group,
                            connectionStatus = connectionStatuses[group.chatId] ?: GroupConnectionStatus.Disconnected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onGroupClick(group)
                            },
                            onLongClick = {
                                showLeaveDialog = group
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCreateGroupClick()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.create_group))
                    }
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onJoinGroupClick()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.join_group))
                    }
                }
            }
        }
    }

    if (showLeaveDialog != null) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = null },
            title = { Text(stringResource(R.string.group_leave)) },
            text = { Text(stringResource(R.string.group_leave_confirm, showLeaveDialog!!.name)) },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog?.let { onLeaveGroup(it) }
                    showLeaveDialog = null
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupItemCard(
    group: Group,
    connectionStatus: GroupConnectionStatus,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                Box(
                    modifier = Modifier.size(48.dp)
                ) {
                    val isOnline = connectionStatus == GroupConnectionStatus.Connected
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                if (isOnline) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isOnline) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(2.dp)
                    ) {
                        val dotColor = when (connectionStatus) {
                            GroupConnectionStatus.Connected -> Color(0xFF4CAF50)
                            GroupConnectionStatus.Connecting,
                            GroupConnectionStatus.Reconnecting -> Color(0xFFFFA726)
                            GroupConnectionStatus.Disconnected -> Color(0xFF9E9E9E)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name.ifEmpty { stringResource(R.string.contact_default_name) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                val topic = group.topic
                if (!topic.isNullOrEmpty()) {
                    Text(
                        text = topic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = stringResource(R.string.group_peer_count, group.peerCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val statusText = when (connectionStatus) {
                    GroupConnectionStatus.Connected -> stringResource(R.string.group_connected)
                    GroupConnectionStatus.Connecting,
                    GroupConnectionStatus.Reconnecting -> stringResource(R.string.group_connecting)
                    GroupConnectionStatus.Disconnected -> stringResource(R.string.group_offline)
                }
                val statusColor = when (connectionStatus) {
                    GroupConnectionStatus.Connected -> Color(0xFF4CAF50)
                    GroupConnectionStatus.Connecting,
                    GroupConnectionStatus.Reconnecting -> Color(0xFFFFA726)
                    GroupConnectionStatus.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = statusColor
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 76.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    }
}
