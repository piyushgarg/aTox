// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.atox.infrastructure.media.VoiceMessagePlayer
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.transfer.model.isComplete
import ltd.evilcorp.domain.features.transfer.model.isStarted
import ltd.evilcorp.domain.features.chat.model.Message

private const val TAG = "VoiceMessageCard"
private const val PLAYBACK_DELAY_MS = 100L
private const val MILLIS_IN_SECOND = 1000
private const val SECONDS_IN_MINUTE = 60

@Composable
fun VoiceMessageCard(
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
    var duration by remember { mutableIntStateOf(0) }

    val isComplete = ft.isComplete() || isOutgoing
    val isStarted = ft.isStarted()

    DisposableEffect(Unit) {
        onDispose {
            val audioPath = if (ft.destination.isNotEmpty()) ft.destination else ft.fileName
            if (VoiceMessagePlayer.isPlayingUri(audioPath)) {
                // Keep playing globally on scroll
            }
        }
    }

    LaunchedEffect(ft.destination, ft.fileName, isComplete) {
        withContext(Dispatchers.IO) {
            val audioPath = if (ft.destination.isNotEmpty()) ft.destination else ft.fileName
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
                    retriever.release()
                    val durMs = durStr?.toIntOrNull() ?: 0
                    if (durMs > 0) {
                        duration = durMs
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to extract media duration", e)
                }
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val audioPath = if (ft.destination.isNotEmpty()) ft.destination else ft.fileName
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
        val audioPath = if (ft.destination.isNotEmpty()) ft.destination else ft.fileName
        if (isPlaying) {
            VoiceMessagePlayer.pause()
            isPlaying = false
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
                duration = VoiceMessagePlayer.getDuration()
                isPlaying = true
            } else {
                Toast.makeText(context, context.getString(R.string.voice_message_not_ready), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val progress = if (ft.fileSize > 0) ft.progress.toFloat() / ft.fileSize.toFloat() else 0f
    val timerText = if (isPlaying) {
        formatDuration(currentPosition)
    } else {
        if (duration > 0) formatDuration(duration) else "Voice"
    }
    val timeString = remember(msg.timestamp, uiConfig.timeFormatPreference) {
        val time = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp
        formatChatTime(context, time, uiConfig.timeFormatPreference)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(220.dp)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        PlaybackControl(
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

        VoiceMessageProgressColumn(
            playbackProgress = playbackProgress,
            timerText = timerText,
            timeString = timeString,
            isOutgoing = isOutgoing,
            isTimestampZero = msg.timestamp == 0L,
            contentColor = contentColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlaybackControl(
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
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isComplete) {
            if (!isStarted) {
                IconButton(
                    onClick = onAcceptFt,
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
                        onClick = onRejectFt,
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
                    onClick = onPlayPause,
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
private fun VoiceMessageProgressColumn(
    playbackProgress: Float,
    timerText: String,
    timeString: String,
    isOutgoing: Boolean,
    isTimestampZero: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        LinearProgressIndicator(
            progress = { playbackProgress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = contentColor,
            trackColor = contentColor.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
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
