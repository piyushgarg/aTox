// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CallHistoryCard(
    title: String,
    timeString: String,
    isOutgoing: Boolean,
    missed: Boolean,
    cancelled: Boolean,
    onClick: () -> Unit,
) {
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start
    val containerColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
    }
    val titleColor = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant
    val statusIcon = when {
        missed -> Icons.AutoMirrored.Filled.PhoneMissed
        isOutgoing || cancelled -> Icons.AutoMirrored.Filled.CallMade
        else -> Icons.AutoMirrored.Filled.CallReceived
    }
    val statusTint = when {
        missed -> MaterialTheme.colorScheme.error
        isOutgoing -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isOutgoing) 20.dp else 8.dp,
                bottomEnd = if (isOutgoing) 8.dp else 20.dp,
            ),
            color = containerColor,
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusTint,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.bodySmall,
                            color = metaColor,
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                ) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = statusTint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
