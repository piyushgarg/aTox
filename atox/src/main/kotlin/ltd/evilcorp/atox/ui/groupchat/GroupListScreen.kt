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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.foundation.lazy.rememberLazyListState
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusOffline
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.domain.model.Group
import ltd.evilcorp.atox.ui.common.AtoxConfirmDialog
import ltd.evilcorp.atox.ui.groupchat.components.GroupItemCard

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
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.no_groups),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_groups_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCreateGroupClick()
                    },
                    modifier = Modifier
                        .width(220.dp)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.create_group))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onJoinGroupClick()
                    },
                    modifier = Modifier
                        .width(220.dp)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.join_group))
                }
            }
        } else {
            val lazyListState = rememberLazyListState()

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = 16.dp
                    )
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


            }
        }
    }

    showLeaveDialog?.let { group ->
        AtoxConfirmDialog(
            onDismiss = { showLeaveDialog = null },
            onConfirm = {
                onLeaveGroup(group)
                showLeaveDialog = null
            },
            title = stringResource(R.string.group_leave),
            text = stringResource(R.string.group_leave_confirm, group.name),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(android.R.string.cancel),
            isDangerous = true
        )
    }
}
