// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.delay
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.stripReplyPrefix
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.Sender

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ChatInputBar(
    contact: Contact?,
    settings: Settings,
    systemSoundPlayer: SystemSoundPlayer,
    onSendMessage: (String) -> Unit,
    onTypingChanged: (Boolean) -> Unit,
    onAttachClick: () -> Unit,
    onHaptic: () -> Unit,
    replyingToMessage: Message?,
    onCancelReply: () -> Unit,
    onSendVoice: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Voice message recording states
    var isRecording by remember { mutableStateOf(false) }
    var recordDuration by remember { mutableIntStateOf(0) }
    val cancelThreshold = with(LocalDensity.current) { 60.dp.toPx() }

    // MediaRecorder local state helpers
    val recordingState = remember { RecordingState() }

    val micScale by animateFloatAsState(
        targetValue = if (isRecording) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "micScale"
    )

    val micBgColor by animateColorAsState(
        targetValue = if (isRecording) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "micBgColor"
    )

    val micContentColor by animateColorAsState(
        targetValue = if (isRecording) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "micContentColor"
    )

    // Check mic permission dynamically
    val checkMicPermission = {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val requestMicPermission = {
        val activity = context as? android.app.Activity
        activity?.let {
            androidx.core.app.ActivityCompat.requestPermissions(
                it,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                200
            )
        }
    }

    // Stopwatch for voice recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordDuration = 0
            while (isRecording) {
                delay(1000)
                recordDuration++
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 1. Reply Preview Block above text field
        AnimatedVisibility(
            visible = replyingToMessage != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(200, easing = AToxMotion.EmphasizedDecelerate)
            ) + fadeIn(animationSpec = tween(150)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(150, easing = AToxMotion.EmphasizedAccelerate)
            ) + fadeOut(animationSpec = tween(100))
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

        // 2. Main Input Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRecording) {
                // Recording status stopwatch panel
                Row(
                    modifier = Modifier
                        .weight(1f)
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
                            animation = tween(800, easing = AToxMotion.CallPulseEasing),
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
                        text = String.format(java.util.Locale.US, "%d:%02d", recordDuration / 60, recordDuration % 60),
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
            } else {
                // Text input field
                TextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        onTypingChanged(it.isNotBlank())
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.chat_write_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                    ),
                    maxLines = 4,
                    trailingIcon = {
                        val isOnline = contact?.connectionStatus != ConnectionStatus.None
                        IconButton(onClick = {
                            onHaptic()
                            if (isOnline) {
                                onAttachClick()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.cannot_send_file_offline),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Attachment,
                                contentDescription = "Attach File",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(rotationZ = -45f)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Dynamic morphing mic / send action button
            val hasText = textInput.trim().isNotEmpty()
            AnimatedContent(
                targetState = hasText,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150, easing = AToxMotion.EmphasizedDecelerate)) with
                    fadeOut(animationSpec = tween(100, easing = AToxMotion.EmphasizedAccelerate))
                },
                label = "morphButton"
            ) { isSendMode ->
                if (isSendMode) {
                    FilledIconButton(
                        onClick = {
                            if (textInput.trim().isNotEmpty()) {
                                onHaptic()
                                onSendMessage(textInput)
                                systemSoundPlayer.playSentSound(settings.sentMessageSoundUri, settings.sentMessageSoundVolume)
                                onTypingChanged(false)
                                textInput = ""
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
                    }
                } else {
                    // Hold to record voice message mic button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = micScale
                                scaleY = micScale
                            }
                            .clip(CircleShape)
                            .background(micBgColor)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown()
                                        if (!checkMicPermission()) {
                                            requestMicPermission()
                                            continue
                                        }

                                        // Start recording process
                                        isRecording = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        recordingState.startRecording(context)

                                        var cancelled = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val currentX = event.changes.firstOrNull()?.position?.x ?: 0f
                                            val startX = down.position.x
                                            val slideDistance = startX - currentX

                                            if (slideDistance > cancelThreshold) {
                                                cancelled = true
                                                isRecording = false
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                recordingState.cancelRecording()
                                                Toast.makeText(context, context.getString(R.string.slide_to_cancel), Toast.LENGTH_SHORT).show()
                                                break
                                            }

                                            if (event.changes.all { !it.pressed }) {
                                                break
                                            }
                                        }

                                        if (isRecording) {
                                            isRecording = false
                                            val file = recordingState.stopRecording()
                                            if (file != null && file.exists() && file.length() > 0) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onSendVoice(android.net.Uri.fromFile(file))
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Message",
                            tint = micContentColor
                        )
                    }
                }
            }
        }
    }
}

// MediaRecorder state class
private class RecordingState {
    private var mediaRecorder: android.media.MediaRecorder? = null
    private var voiceFile: java.io.File? = null

    fun startRecording(context: android.content.Context) {
        val file = java.io.File(context.cacheDir, "voice_message_${System.currentTimeMillis()}.m4a")
        voiceFile = file
        try {
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }.apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording(): java.io.File? {
        val file = voiceFile
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        voiceFile = null
        return file
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        voiceFile?.delete()
        voiceFile = null
    }
}
