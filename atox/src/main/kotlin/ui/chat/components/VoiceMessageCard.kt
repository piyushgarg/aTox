// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.common.formatChatTime
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.isComplete
import ltd.evilcorp.core.model.isStarted
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.Sender

@Composable
fun VoiceMessageCard(
    ft: FileTransfer,
    contentColor: Color,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    msg: Message,
    isOutgoing: Boolean,
    settings: Settings
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }

    val isComplete = ft.isComplete()
    val isStarted = ft.isStarted()

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
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
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer != null) {
                try {
                    val pos = mediaPlayer?.currentPosition ?: 0
                    val dur = mediaPlayer?.duration ?: 1
                    playbackProgress = pos.toFloat() / dur.toFloat()
                    currentPosition = pos
                    delay(100)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    val playPauseAudio = {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        } else {
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
                if (mediaPlayer == null) {
                    try {
                        val mp = android.media.MediaPlayer().apply {
                            if (uri.scheme == "content" || uri.scheme == "file") {
                                setDataSource(context, uri)
                            } else {
                                val file = java.io.File(audioPath)
                                setDataSource(context, android.net.Uri.fromFile(file))
                            }
                            prepare()
                        }
                        duration = mp.duration
                        mp.setOnCompletionListener {
                            isPlaying = false
                            playbackProgress = 0f
                            currentPosition = 0
                            mp.seekTo(0)
                        }
                        mediaPlayer = mp
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Не удалось воспроизвести аудиофайл", Toast.LENGTH_SHORT).show()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        isPlaying = false
                    }
                }
                
                if (mediaPlayer != null) {
                    mediaPlayer?.start()
                    isPlaying = true
                }
            } else {
                Toast.makeText(context, "Аудиофайл еще загружается или не доступен", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(220.dp)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isComplete) {
                if (!isStarted) {
                    IconButton(
                        onClick = { onAcceptFt(ft.id) },
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
                    val progress = if (ft.fileSize > 0) ft.progress.toFloat() / ft.fileSize.toFloat() else 0f
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
                            onClick = { onRejectFt(ft.id) },
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
                        onClick = { playPauseAudio() },
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

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            LinearProgressIndicator(
                progress = { playbackProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
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
                val timerText = if (isPlaying) {
                    formatDuration(currentPosition)
                } else {
                    if (duration > 0) formatDuration(duration) else "Voice"
                }
                Text(
                    text = timerText,
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    val timeString = remember(msg.timestamp, settings.timeFormatPreference) {
                        val time = if (msg.timestamp == 0L) System.currentTimeMillis() else msg.timestamp
                        formatChatTime(context, time, settings.timeFormatPreference)
                    }
                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        if (msg.timestamp == 0L) {
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
}

private fun formatDuration(ms: Int): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(java.util.Locale.US, "%d:%02d", mins, secs)
}
