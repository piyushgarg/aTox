package ltd.evilcorp.atox.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.ui.chat.components.CallHistoryCard
import ltd.evilcorp.atox.ui.chat.components.FileTransferCard
import ltd.evilcorp.atox.ui.chat.components.TypingBubble
import ltd.evilcorp.atox.ui.theme.AToxTheme
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.Sender

@Preview(name = "Typing Bubble", showBackground = true)
@Composable
private fun TypingBubblePreview() {
    AToxTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TypingBubble()
        }
    }
}

@Preview(name = "Call History Cards", showBackground = true)
@Composable
private fun CallHistoryCardsPreview() {
    AToxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CallHistoryCard(
                title = "Incoming call (5 min)",
                timeString = "12:34",
                isOutgoing = false,
                onClick = {}
            )
            CallHistoryCard(
                title = "Outgoing call (missed)",
                timeString = "Yesterday",
                isOutgoing = true,
                onClick = {}
            )
        }
    }
}

@Preview(name = "File Transfer Cards", showBackground = true)
@Composable
private fun FileTransferCardsPreview() {
    AToxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val incomingFt = FileTransfer(
                publicKey = "key",
                fileNumber = 1,
                fileKind = 0,
                fileSize = 1024 * 1024 * 5, // 5MB
                fileName = "presentation.pdf",
                outgoing = false,
                progress = 1024 * 1024 * 2, // 2MB progress
                destination = "",
                id = 1
            )
            val msgIncoming = Message(
                publicKey = "key",
                message = "Sending presentation.pdf",
                sender = Sender.Received,
                type = MessageType.FileTransfer,
                correlationId = 1,
                timestamp = System.currentTimeMillis()
            )
            FileTransferCard(
                ft = incomingFt,
                msg = msgIncoming,
                onHaptic = {},
                contentColor = Color.Black,
                onAcceptFt = {},
                onRejectFt = {},
                onCancelFt = {},
                onSaveAsClick = { _, _ -> },
                onOpenFile = {}
            )

            val outgoingFt = FileTransfer(
                publicKey = "key",
                fileNumber = 2,
                fileKind = 0,
                fileSize = 1024 * 100, // 100KB
                fileName = "avatar.png",
                outgoing = true,
                progress = 1024 * 100, // Complete
                destination = "",
                id = 2
            )
            val msgOutgoing = Message(
                publicKey = "key",
                message = "Sent avatar.png",
                sender = Sender.Sent,
                type = MessageType.FileTransfer,
                correlationId = 2,
                timestamp = System.currentTimeMillis()
            )
            FileTransferCard(
                ft = outgoingFt,
                msg = msgOutgoing,
                onHaptic = {},
                contentColor = Color.White,
                onAcceptFt = {},
                onRejectFt = {},
                onCancelFt = {},
                onSaveAsClick = { _, _ -> },
                onOpenFile = {}
            )
        }
    }
}
