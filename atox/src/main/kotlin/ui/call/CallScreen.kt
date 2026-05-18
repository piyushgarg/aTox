package ltd.evilcorp.atox.ui.call

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.theme.ContactBackgrounds
import ltd.evilcorp.atox.ui.theme.avatarContentColor
import ltd.evilcorp.core.model.Contact
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import ltd.evilcorp.atox.util.PermissionManager

@Composable
fun CallScreen(
    contactState: State<Contact?>,
    sendingAudioState: State<Boolean>,
    speakerphoneOnState: State<Boolean>,
    permissionManager: PermissionManager,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    val contact = contactState.value

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!sendingAudioState.value) {
                onToggleMic()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.call_mic_permission_needed), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionManager.canRecordAudio()) {
            permissionLauncher.launch(permissionManager.recordAudioPermission)
        }
    }

    val name = contact?.name?.ifEmpty { stringResource(R.string.contact_default_name) } 
        ?: stringResource(R.string.contact_default_name)
    val initials = remember(name) {
        val segments = name.split(" ")
        if (segments.size == 1) name.take(1) else name.take(1) + segments[1].take(1)
    }

    val avatarColor = remember(contact?.publicKey) {
        val key = contact?.publicKey ?: ""
        ContactBackgrounds[abs(key.hashCode()).rem(ContactBackgrounds.size)]
    }

    // Call ring animation (pulsing circles)
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Pulse Ring Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                // Pulsing outer circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            alpha = alpha
                        )
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.4f))
                )

                // Main circular avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials.uppercase(),
                        color = avatarContentColor(avatarColor),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.call_screen_calling),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
        }

        // Control buttons row at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Microphone Toggle Button
                val isMicMuted = !sendingAudioState.value
                val micIcon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic
                val micTint = if (isMicMuted) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                val micBg = if (isMicMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                
                IconButton(
                    onClick = {
                        if (permissionManager.canRecordAudio()) {
                            onToggleMic()
                        } else {
                            permissionLauncher.launch(permissionManager.recordAudioPermission)
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(micBg)
                ) {
                    Icon(
                        imageVector = micIcon,
                        contentDescription = "Toggle Microphone",
                        tint = micTint,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Drop Call Button (Red, in the center)
                FilledIconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.end_call),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Speakerphone Toggle Button
                val isSpeakerOn = speakerphoneOnState.value
                val speakerIcon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff
                val speakerTint = if (isSpeakerOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                val speakerBg = if (isSpeakerOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(speakerBg)
                ) {
                    Icon(
                        imageVector = speakerIcon,
                        contentDescription = "Toggle Speakerphone",
                        tint = speakerTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
