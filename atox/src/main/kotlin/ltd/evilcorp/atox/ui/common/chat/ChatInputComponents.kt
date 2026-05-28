package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.stripReplyPrefix
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.Sender

private const val VOICE_RECORDING_PULSE_MS = 800
private const val SECONDS_IN_MINUTE = 60

@Composable
fun ReplyPreviewHeader(
    replyingToMessage: Message?,
    contact: Contact?,
    onCancelReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = replyingToMessage != null,
        enter = AToxMotion.replyPreviewEnter(),
        exit = AToxMotion.replyPreviewExit(),
        modifier = modifier
    ) {
        replyingToMessage?.let { replyMsg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (replyMsg.sender == Sender.Sent) {
                            stringResource(R.string.reply_you)
                        } else {
                            contact?.name?.ifEmpty { stringResource(R.string.contact_default_name) }
                                ?: stringResource(R.string.contact_default_name)
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stripReplyPrefix(replyMsg.message),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel reply",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingStopwatchPanel(
    recordDuration: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(VOICE_RECORDING_PULSE_MS, easing = AToxMotion.CallPulseEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer(alpha = pulseAlpha)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = String.format(java.util.Locale.US, "%d:%02d", recordDuration / SECONDS_IN_MINUTE, recordDuration % SECONDS_IN_MINUTE),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.slide_to_cancel) + " <-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
        )
    }
}
