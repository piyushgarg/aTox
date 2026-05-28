// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat.components

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.atox.ui.common.MorphingNavigationIcon
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusBusy
import ltd.evilcorp.atox.ui.theme.StatusOffline
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatAppBar(
    contact: Contact?,
    contactName: String,
    presenceInfoText: String,
    connectionStatus: ConnectionStatus?,
    userStatus: UserStatus?,
    isExpanded: Boolean,
    transitionElevation: Dp,
    transitionAlpha: Float,
    onBack: () -> Unit,
    onCallClick: () -> Unit,
    performHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        tonalElevation = transitionElevation,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = transitionAlpha),
        modifier = modifier.fillMaxWidth()
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                performHaptic()
                                val pk = contact?.publicKey ?: ""
                                if (pk.isNotEmpty()) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("friend ID", pk)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.profile_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                ) {
                    ContactAvatar(
                        name = contactName,
                        publicKey = contact?.publicKey ?: "",
                        avatarUri = contact?.avatarUri ?: "",
                        size = 36.dp,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = contactName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = when (connectionStatus) {
                                            ConnectionStatus.None -> StatusOffline
                                            else -> when (userStatus) {
                                                UserStatus.Away -> StatusAway
                                                UserStatus.Busy -> StatusBusy
                                                else -> StatusAvailable
                                            }
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = presenceInfoText,
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
                        MorphingNavigationIcon(
                            isBack = true,
                            onClick = {
                                performHaptic()
                                onBack()
                            }
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = {
                    performHaptic()
                    onCallClick()
                }) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            )
        )
    }
}
