// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.groupchat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusOffline

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupItemCard(
    group: Group,
    connectionStatus: GroupConnectionStatus,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
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
                        imageVector = Icons.Default.Group,
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
                        GroupConnectionStatus.Connected -> StatusAvailable
                        GroupConnectionStatus.Connecting,
                        GroupConnectionStatus.Reconnecting -> StatusAway
                        GroupConnectionStatus.Disconnected -> StatusOffline
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
                    GroupConnectionStatus.Connected -> StatusAvailable
                    GroupConnectionStatus.Connecting,
                    GroupConnectionStatus.Reconnecting -> StatusAway
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
