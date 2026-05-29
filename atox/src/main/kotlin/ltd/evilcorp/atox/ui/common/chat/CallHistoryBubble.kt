// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.ui.chat.components.CallHistoryCard
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.domain.features.chat.model.Message

@Composable
fun CallHistoryBubble(
    msg: Message,
    isOutgoing: Boolean,
    uiConfig: ChatUiConfig,
    onHaptic: () -> Unit,
    onCallHistoryClick: () -> Unit
) {
    val context = LocalContext.current
    val timeString = remember(msg.timestamp, uiConfig.timeFormatPreference) {
        val time = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp
        formatChatTime(context, time, uiConfig.timeFormatPreference)
    }
    val displayTitle = remember(msg.message) {
        when (msg.message) {
            "[CALL_HISTORY_OUTGOING]" -> context.getString(R.string.call_history_outgoing)
            "[CALL_HISTORY_INCOMING]" -> context.getString(R.string.call_history_incoming)
            "[CALL_HISTORY_MISSED]" -> context.getString(R.string.call_history_missed)
            "[CALL_HISTORY_CANCELLED]" -> context.getString(R.string.call_history_cancelled)
            
            // English compatibility
            "Outgoing call" -> context.getString(R.string.call_history_outgoing)
            "Incoming call" -> context.getString(R.string.call_history_incoming)
            "Missed call" -> context.getString(R.string.call_history_missed)
            "Cancelled call" -> context.getString(R.string.call_history_cancelled)
            
            // Russian compatibility
            "Исходящий звонок" -> context.getString(R.string.call_history_outgoing)
            "Входящий звонок" -> context.getString(R.string.call_history_incoming)
            "Пропущенный звонок" -> context.getString(R.string.call_history_missed)
            "Отменённый звонок", "Отмененный звонок" -> context.getString(R.string.call_history_cancelled)
            
            else -> msg.message
        }
    }
    val missed = remember(msg.message) {
        msg.message == "[CALL_HISTORY_MISSED]" ||
        msg.message == "Missed call" ||
        msg.message == "Пропущенный звонок"
    }
    val cancelled = remember(msg.message) {
        msg.message == "[CALL_HISTORY_CANCELLED]" ||
        msg.message == "Cancelled call" ||
        msg.message == "Отменённый звонок" ||
        msg.message == "Отмененный звонок"
    }
    CallHistoryCard(
        title = displayTitle,
        timeString = timeString,
        isOutgoing = isOutgoing,
        missed = missed,
        cancelled = cancelled,
        onClick = {
            onHaptic()
            onCallHistoryClick()
        },
    )
}
