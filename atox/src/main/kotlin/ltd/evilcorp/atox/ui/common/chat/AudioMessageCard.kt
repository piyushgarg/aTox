@file:OptIn(ExperimentalMaterial3Api::class)

package ltd.evilcorp.atox.ui.common.chat

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.media.VoiceMessagePlayer
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.transfer.model.isComplete
import ltd.evilcorp.domain.features.transfer.model.isStarted

private const val TAG = "AudioMessageCard"
private const val PLAYBACK_DELAY_MS = 100L
private const val MILLIS_IN_SECOND = 1000
private const val SECONDS_IN_MINUTE = 60

data class AudioMetadata(
    val artist: String,
    val title: String,
    val durationMs: Int
)

@Composable
fun AudioMessageCard(
    ft: FileTransfer,
    contentColor: Color,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    msg: Message,
    isOutgoing: Boolean,
    uiConfig: ChatUiConfig
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var metadata by remember { mutableStateOf(AudioMetadata("<unknown>", "<unknown>", 0)) }

    val isComplete = ft.isComplete() || isOutgoing
    val isStarted = ft.isStarted()

    val audioPath = if (ft.destination.isNotEmpty()) ft.destination else ft.fileName

    // Sync isPlaying state on recomposition (e.g. after scroll)
    LaunchedEffect(audioPath) {
        if (VoiceMessagePlayer.isPlayingUri(audioPath)) {
            isPlaying = true
            currentPosition = VoiceMessagePlayer.getCurrentPosition()
            val dur = VoiceMessagePlayer.getDuration().coerceAtLeast(1)
            playbackProgress = currentPosition.toFloat() / dur.toFloat()
        }
    }

    LaunchedEffect(ft.destination, ft.fileName, isComplete) {
        withContext(Dispatchers.IO) {
            val uri = android.net.Uri.parse(audioPath)
            val exists = try {
                when (uri.scheme) {
                    "content" -> {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length > 0 } ?: false
                    }
                    "file" -> {
                        val file = java.io.File(uri.path ?: audioPath)
                        file.exists() && file.length() > 0
                    }
                    else -> {
                        val file = java.io.File(audioPath)
                        file.exists() && file.length() > 0
                    }
                }
            } catch (e: Exception) {
                false
            }

            if (exists) {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    if (uri.scheme == "content" || uri.scheme == "file") {
                        retriever.setDataSource(context, uri)
                    } else {
                        val file = java.io.File(audioPath)
                        retriever.setDataSource(context, android.net.Uri.fromFile(file))
                    }
                    val durStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                    retriever.release()

                    val durMs = durStr?.toIntOrNull() ?: 0
                    metadata = AudioMetadata(
                        artist = artist?.takeIf { it.isNotBlank() } ?: "<unknown>",
                        title = title?.takeIf { it.isNotBlank() } ?: "<unknown>",
                        durationMs = durMs
                    )
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to extract audio metadata", e)
                }
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && VoiceMessagePlayer.isPlayingUri(audioPath)) {
                try {
                    val pos = VoiceMessagePlayer.getCurrentPosition()
                    val dur = VoiceMessagePlayer.getDuration().coerceAtLeast(1)
                    playbackProgress = pos.toFloat() / dur.toFloat()
                    currentPosition = pos
                    delay(PLAYBACK_DELAY_MS)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    val playPauseAudio = {
        if (isPlaying) {
            VoiceMessagePlayer.pause()
            isPlaying = false
        } else {
            if (VoiceMessagePlayer.isPlayingUri(audioPath)) {
                VoiceMessagePlayer.resume()
                isPlaying = true
            } else {
                val uri = android.net.Uri.parse(audioPath)
                val exists = try {
                    when (uri.scheme) {
                        "content" -> {
                            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length > 0 } ?: false
                        }
                        "file" -> {
                            val file = java.io.File(uri.path ?: audioPath)
                            file.exists() && file.length() > 0
                        }
                        else -> {
                            val file = java.io.File(audioPath)
                            file.exists() && file.length() > 0
                        }
                    }
                } catch (e: Exception) {
                    false
                }
                if (exists) {
                    VoiceMessagePlayer.play(
                        context = context,
                        uriString = audioPath,
                        onComplete = {
                            isPlaying = false
                            playbackProgress = 0f
                            currentPosition = 0
                        },
                        onError = {
                            isPlaying = false
                            Toast.makeText(context, context.getString(R.string.voice_message_play_error), Toast.LENGTH_SHORT).show()
                        }
                    )
                    isPlaying = true
                } else {
                    Toast.makeText(context, context.getString(R.string.voice_message_not_ready), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val seekToPosition: (Float) -> Unit = { fraction ->
        val dur = VoiceMessagePlayer.getDuration().coerceAtLeast(1)
        val targetMs = (fraction * dur).toInt().coerceIn(0, dur)
        VoiceMessagePlayer.seekTo(targetMs)
        playbackProgress = fraction
        currentPosition = targetMs
    }

    val progress = if (ft.fileSize > 0) ft.progress.toFloat() / ft.fileSize.toFloat() else 0f
    val timerText = if (isPlaying || currentPosition > 0) {
        formatDuration(currentPosition)
    } else {
        if (metadata.durationMs > 0) formatDuration(metadata.durationMs) else "Audio"
    }
    val timeString = remember(msg.timestamp, uiConfig.timeFormatPreference) {
        val time = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp
        formatChatTime(context, time, uiConfig.timeFormatPreference)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(260.dp)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        AudioPlaybackControl(
            isComplete = isComplete,
            isStarted = isStarted,
            isPlaying = isPlaying,
            progress = progress,
            contentColor = contentColor,
            onAcceptFt = { onAcceptFt(ft.id) },
            onRejectFt = { onRejectFt(ft.id) },
            onPlayPause = playPauseAudio
        )

        Spacer(modifier = Modifier.width(8.dp))

        AudioMessageProgressColumn(
            playbackProgress = playbackProgress,
            timerText = timerText,
            timeString = timeString,
            isOutgoing = isOutgoing,
            isTimestampZero = msg.timestamp == 0L,
            contentColor = contentColor,
            artist = metadata.artist,
            title = metadata.title,
            onSeek = seekToPosition,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AudioPlaybackControl(
    isComplete: Boolean,
    isStarted: Boolean,
    isPlaying: Boolean,
    progress: Float,
    contentColor: Color,
    onAcceptFt: () -> Unit,
    onRejectFt: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isComplete) {
            if (!isStarted) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAcceptFt()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Download audio",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = contentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRejectFt()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel download",
                            tint = contentColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = contentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayPause()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioMessageProgressColumn(
    playbackProgress: Float,
    timerText: String,
    timeString: String,
    isOutgoing: Boolean,
    isTimestampZero: Boolean,
    contentColor: Color,
    artist: String,
    title: String,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist,
            fontSize = 11.sp,
            color = contentColor.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        var sliderPosition by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        Slider(
            value = if (isDragging) sliderPosition else playbackProgress,
            onValueChange = {
                isDragging = true
                sliderPosition = it
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek(sliderPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = contentColor,
                activeTrackColor = contentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    colors = SliderDefaults.colors(thumbColor = contentColor),
                    thumbSize = DpSize(10.dp, 10.dp)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = contentColor,
                        inactiveTrackColor = contentColor.copy(alpha = 0.2f)
                    ),
                    thumbTrackGapSize = 0.dp
                )
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timerText,
                fontSize = 11.sp,
                color = contentColor.copy(alpha = 0.8f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = timeString,
                    fontSize = 10.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
                if (isOutgoing) {
                    Spacer(modifier = Modifier.width(4.dp))
                    if (isTimestampZero) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Sending",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                    } else {
                        Box(modifier = Modifier.size(width = 16.dp, height = 11.dp)) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Delivered",
                                tint = contentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp).align(Alignment.CenterStart)
                            )
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Read",
                                tint = contentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp).align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Int): String {
    val totalSecs = ms / MILLIS_IN_SECOND
    return String.format(java.util.Locale.US, "%d:%02d", totalSecs / SECONDS_IN_MINUTE, totalSecs % SECONDS_IN_MINUTE)
}
