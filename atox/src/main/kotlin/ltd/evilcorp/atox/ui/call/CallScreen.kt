package ltd.evilcorp.atox.ui.call

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.domain.features.call.CallState

@Composable
fun CallScreen(
    publicKey: String,
    contact: Contact?,
    callState: CallState,
    sendingAudio: Boolean,
    speakerphoneOn: Boolean,
    callDuration: String,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onMinimize: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
    hapticEnabled: Boolean,
) {
    BackHandler(onBack = onMinimize)

    val currentCallState = callState
    val durationText = callDuration

    val name = contact?.name?.ifEmpty { publicKey.take(ltd.evilcorp.domain.core.model.FINGERPRINT_LEN) }
        ?: publicKey.take(ltd.evilcorp.domain.core.model.FINGERPRINT_LEN)

    val statusText = when (currentCallState) {
        is CallState.OutgoingRequesting -> stringResource(R.string.call_screen_requesting)
        is CallState.OutgoingWaiting -> stringResource(R.string.call_screen_waiting)
        is CallState.Connecting -> stringResource(R.string.call_screen_connecting)
        is CallState.OutgoingRinging -> stringResource(R.string.call_screen_ringing)
        is CallState.Active -> durationText
        is CallState.IncomingRinging -> stringResource(R.string.incoming_call)
        CallState.Idle -> stringResource(R.string.call_screen_calling)
    }

    CallScreenContent(
        publicKey = publicKey,
        contact = contact,
        name = name,
        sendingAudio = sendingAudio,
        speakerphoneOn = speakerphoneOn,
        statusText = statusText,
        hasMicPermission = hasMicPermission,
        onRequestMicPermission = onRequestMicPermission,
        onMinimize = onMinimize,
        onToggleMic = onToggleMic,
        onToggleSpeaker = onToggleSpeaker,
        onEndCall = onEndCall,
        hapticEnabled = hapticEnabled,
    )
}

@Composable
fun CallScreenContent(
    publicKey: String,
    contact: Contact?,
    name: String,
    sendingAudio: Boolean,
    speakerphoneOn: Boolean,
    statusText: String,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onMinimize: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
    hapticEnabled: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val performHaptic = {
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!sendingAudio) {
                onToggleMic()
            }
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            colorScheme.primaryContainer,
            colorScheme.surfaceContainerLow,
            colorScheme.background,
        ),
    )
    val onBackground = colorScheme.onBackground
    val panelColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
    val ringColor = colorScheme.primary.copy(alpha = 0.22f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = {
                performHaptic()
                onMinimize()
            },
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = onBackground,
            ),
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.return_to_chat), modifier = Modifier.size(32.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .clip(CircleShape)
                        .background(ringColor)
                )
                Surface(
                    shape = CircleShape,
                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.78f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(144.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ContactAvatar(
                            name = name,
                            publicKey = publicKey,
                            avatarUri = contact?.avatarUri.orEmpty(),
                            size = 124.dp,
                            fontSize = 40.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = name,
                color = onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = panelColor,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary),
                    )
                    Text(
                        text = statusText,
                        color = colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

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
                val isMicMuted = !sendingAudio
                val micIcon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic
                val micTint = if (isMicMuted) colorScheme.onError else colorScheme.onSecondaryContainer
                val micBg = if (isMicMuted) colorScheme.error else colorScheme.secondaryContainer

                IconButton(
                    onClick = {
                        performHaptic()
                        if (hasMicPermission) {
                            onToggleMic()
                        } else {
                            onRequestMicPermission()
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(micBg),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = micTint),
                ) {
                    Icon(
                        imageVector = micIcon,
                        contentDescription = stringResource(R.string.microphone_control),
                        tint = micTint,
                        modifier = Modifier.size(26.dp)
                    )
                }

                FilledIconButton(
                    onClick = {
                        performHaptic()
                        onEndCall()
                    },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.end_call),
                        modifier = Modifier.size(36.dp)
                    )
                }

                val isSpeakerOn = speakerphoneOn
                val speakerIcon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff
                val speakerTint = if (isSpeakerOn) colorScheme.onPrimary else colorScheme.onSecondaryContainer
                val speakerBg = if (isSpeakerOn) colorScheme.primary else colorScheme.secondaryContainer

                IconButton(
                    onClick = {
                        performHaptic()
                        onToggleSpeaker()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(speakerBg),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = speakerTint),
                ) {
                    Icon(
                        imageVector = speakerIcon,
                        contentDescription = stringResource(R.string.speakerphone_toggle),
                        tint = speakerTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CallScreenOutgoingPreview() {
    MaterialTheme {
        CallScreenContent(
            publicKey = "123",
            contact = Contact(name = "Alice", publicKey = "123"),
            name = "Alice",
            sendingAudio = true,
            speakerphoneOn = false,
            statusText = "Requesting...",
            hasMicPermission = true,
            onRequestMicPermission = {},
            onMinimize = {},
            onToggleMic = {},
            onToggleSpeaker = {},
            onEndCall = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CallScreenActivePreview() {
    MaterialTheme {
        CallScreenContent(
            publicKey = "456",
            contact = Contact(name = "Bob", publicKey = "456"),
            name = "Bob",
            sendingAudio = true,
            speakerphoneOn = true,
            statusText = "03:45",
            hasMicPermission = true,
            onRequestMicPermission = {},
            onMinimize = {},
            onToggleMic = {},
            onToggleSpeaker = {},
            onEndCall = {}
        )
    }
}
