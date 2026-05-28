// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.domain.features.chat.model.Message

@Composable
fun rememberParsedSystemEvent(message: String): String {
    val context = LocalContext.current
    return remember(message) {
        when {
            message.startsWith("[SYSTEM_EVENT:PEER_JOINED|") && message.endsWith("]") -> {
                val name = message.removePrefix("[SYSTEM_EVENT:PEER_JOINED|").removeSuffix("]")
                context.getString(ltd.evilcorp.atox.R.string.group_peer_joined, name)
            }
            message.startsWith("[SYSTEM_EVENT:PEER_LEFT|") && message.endsWith("]") -> {
                val name = message.removePrefix("[SYSTEM_EVENT:PEER_LEFT|").removeSuffix("]")
                context.getString(ltd.evilcorp.atox.R.string.group_peer_left, name)
            }
            message.startsWith("[SYSTEM_EVENT:PEER_KICKED|") && message.endsWith("]") -> {
                val name = message.removePrefix("[SYSTEM_EVENT:PEER_KICKED|").removeSuffix("]")
                context.getString(ltd.evilcorp.atox.R.string.group_peer_kicked, name)
            }
            else -> message
        }
    }
}

@Composable
fun SystemEventBubble(
    msg: Message,
    uiConfig: ChatUiConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeString = remember(msg.timestamp, uiConfig.timeFormatPreference) {
        val time = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp
        formatChatTime(context, time, uiConfig.timeFormatPreference)
    }
    val displayText = rememberParsedSystemEvent(msg.message)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            tonalElevation = 1.dp,
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = timeString,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontSize = 10.sp,
        )
    }
}
