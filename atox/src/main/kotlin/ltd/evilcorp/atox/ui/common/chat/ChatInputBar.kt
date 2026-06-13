// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.chat.model.Message

private const val VOICE_CANCEL_THRESHOLD_DP = 60
private const val VOICE_STOPWATCH_INTERVAL_MS = 1000L
private const val BUTTON_TOGGLE_DURATION_MS = 300

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ChatInputBar(
    contact: Contact?,
    uiConfig: ChatUiConfig,
    systemSoundPlayer: SystemSoundPlayer,
    onSendMessage: (String) -> Unit,
    onTypingChanged: (Boolean) -> Unit,
    onAttachClick: () -> Unit,
    onHaptic: () -> Unit,
    replyingToMessage: Message?,
    onCancelReply: () -> Unit,
    onSendVoice: (android.net.Uri) -> Unit,
    voiceRecorder: ltd.evilcorp.domain.features.call.service.IVoiceRecorder,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Voice message recording states
    var isRecording by remember { mutableStateOf(false) }
    var recordDuration by remember { mutableIntStateOf(0) }
    val cancelThreshold = with(LocalDensity.current) { VOICE_CANCEL_THRESHOLD_DP.dp.toPx() }

    // Voice recording lifecycle observer
    DisposableEffect(voiceRecorder) {
        onDispose {
            voiceRecorder.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, voiceRecorder) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (isRecording) {
                    coroutineScope.launch {
                        voiceRecorder.cancelRecording()
                    }
                    isRecording = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Check mic permission dynamically and Compose launcher
    val checkMicPermission = {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    var hasMicPermission by remember {
        mutableStateOf(checkMicPermission())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                context.getString(R.string.call_mic_permission_needed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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
        animationSpec = tween(durationMillis = BUTTON_TOGGLE_DURATION_MS, easing = FastOutSlowInEasing),
        label = "micBgColor"
    )

    val micContentColor by animateColorAsState(
        targetValue = if (isRecording) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(durationMillis = BUTTON_TOGGLE_DURATION_MS, easing = FastOutSlowInEasing),
        label = "micContentColor"
    )

    // Stopwatch for voice recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordDuration = 0
            while (isRecording) {
                delay(VOICE_STOPWATCH_INTERVAL_MS)
                recordDuration++
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
    ) {
        // 1. Reply Preview Block above text field
        ReplyPreviewHeader(
            replyingToMessage = replyingToMessage,
            contact = contact,
            onCancelReply = onCancelReply
        )

        // 2. Main Input Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRecording) {
                // Recording status stopwatch panel
                RecordingStopwatchPanel(
                    recordDuration = recordDuration,
                    modifier = Modifier.weight(1f)
                )
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
            if (hasText) {
                FilledIconButton(
                    onClick = {
                        if (textInput.trim().isNotEmpty()) {
                            onHaptic()
                            onSendMessage(textInput)
                            systemSoundPlayer.playSentSound(uiConfig.sentMessageSoundUri, uiConfig.sentMessageSoundVolume)
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
                VoiceMessageRecordButton(
                    isRecording = isRecording,
                    onRecordingStateChanged = { isRecording = it },
                    micScale = micScale,
                    micBgColor = micBgColor,
                    micContentColor = micContentColor,
                    checkMicPermission = checkMicPermission,
                    permissionLauncher = permissionLauncher,
                    voiceRecorder = voiceRecorder,
                    cancelThreshold = cancelThreshold,
                    onSendVoice = onSendVoice,
                    haptic = haptic,
                    context = context
                )
            }
        }
    }
}


