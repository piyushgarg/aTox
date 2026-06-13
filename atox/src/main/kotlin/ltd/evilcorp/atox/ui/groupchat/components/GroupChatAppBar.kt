package ltd.evilcorp.atox.ui.groupchat.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupPeer
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupChatAppBar(
    group: Group?,
    peers: List<GroupPeer>,
    connStatus: GroupConnectionStatus,
    uiConfig: ChatUiConfig,
    onBack: () -> Unit,
    onInviteClick: () -> Unit,
    onPeersClick: () -> Unit,
    onLeaveClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    isExpanded: Boolean = false,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (uiConfig.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onPeersClick()
                            },
                            onLongClick = {
                                if (uiConfig.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                val id = group?.chatId ?: ""
                                if (id.isNotEmpty()) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("group ID", id)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.group_invite_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = group?.name?.ifEmpty {
                                context.getString(R.string.contact_default_name)
                            } ?: context.getString(R.string.contact_default_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = when (connStatus) {
                                GroupConnectionStatus.Connected -> ltd.evilcorp.atox.ui.theme.StatusAvailable
                                GroupConnectionStatus.Connecting,
                                GroupConnectionStatus.Reconnecting -> ltd.evilcorp.atox.ui.theme.StatusAway
                                GroupConnectionStatus.Disconnected -> ltd.evilcorp.atox.ui.theme.StatusOffline
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val statusText = when (connStatus) {
                                GroupConnectionStatus.Connected -> context.getString(R.string.group_connected)
                                GroupConnectionStatus.Connecting,
                                GroupConnectionStatus.Reconnecting -> context.getString(R.string.group_connecting)
                                GroupConnectionStatus.Disconnected -> context.getString(R.string.group_offline)
                            }
                            Text(
                                text = "$statusText • ${context.getString(R.string.group_peer_count, peers.size)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                if (!isExpanded) {
                    Box(modifier = Modifier.padding(start = 4.dp)) {
                        ltd.evilcorp.atox.ui.common.MorphingNavigationIcon(
                            isBack = true,
                            onClick = {
                                if (uiConfig.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onBack()
                            }
                        )
                    }
                }
            },
            actions = {
                Box {
                    IconButton(onClick = {
                        if (uiConfig.hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        menuExpanded = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = context.getString(R.string.more_options),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.group_invite_friend)) },
                            leadingIcon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                if (uiConfig.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onInviteClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.group_info)) },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                if (uiConfig.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onPeersClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(context.getString(R.string.group_leave), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                menuExpanded = false
                                if (uiConfig.hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onLeaveClick()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor
            )
        )
}
