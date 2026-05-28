package ltd.evilcorp.atox.ui.common.chat

import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.call.service.IVoiceRecorder
import java.io.File

@Suppress("LoopWithTooManyJumpStatements", "UnusedParameter")
@Composable
fun VoiceMessageRecordButton(
    isRecording: Boolean,
    onRecordingStateChanged: (Boolean) -> Unit,
    micScale: Float,
    micBgColor: Color,
    micContentColor: Color,
    checkMicPermission: () -> Boolean,
    permissionLauncher: ActivityResultLauncher<String>,
    voiceRecorder: IVoiceRecorder,
    cancelThreshold: Float,
    onSendVoice: (android.net.Uri) -> Unit,
    haptic: HapticFeedback,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
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
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            continue
                        }

                        val success = voiceRecorder.startRecording()
                        if (success) {
                            onRecordingStateChanged(true)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            // Wait until the gesture releases before continuing to prevent spin locking
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })
                            continue
                        }

                        var cancelled = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val currentX = event.changes.firstOrNull()?.position?.x ?: 0f
                            val startX = down.position.x
                            val slideDistance = startX - currentX

                            if (slideDistance > cancelThreshold) {
                                cancelled = true
                                onRecordingStateChanged(false)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                voiceRecorder.cancelRecording()
                                Toast.makeText(context, context.getString(R.string.slide_to_cancel), Toast.LENGTH_SHORT).show()
                                break
                            }

                            if (event.changes.all { !it.pressed }) {
                                break
                            }
                        }

                        if (!cancelled) {
                            onRecordingStateChanged(false)
                            val filePath = voiceRecorder.stopRecording()
                            if (filePath != null) {
                                val file = File(filePath)
                                if (file.exists() && file.length() > 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSendVoice(android.net.Uri.fromFile(file))
                                }
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
