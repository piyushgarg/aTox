package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SWIPE_THRESHOLD = 0.3f
private const val MAX_SWIPE_DP = 60f

@Composable
fun SwipeableReplyBox(
    onReply: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxSwipePx = with(density) { MAX_SWIPE_DP.dp.toPx() }

    val haptic = LocalHapticFeedback.current
    var offsetX by remember { mutableStateOf(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
 
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(offsetX) >= maxSwipePx * SWIPE_THRESHOLD) {
                            onReply()
                        }
                        hasTriggeredHaptic = false
                        scope.launch {
                            animate(offsetX, 0f) { value, _ -> offsetX = value }
                        }
                    },
                    onDragCancel = {
                        hasTriggeredHaptic = false
                        scope.launch {
                            animate(offsetX, 0f) { value, _ -> offsetX = value }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount < 0f) {
                            offsetX = (offsetX + dragAmount).coerceIn(-maxSwipePx, 0f)
                            if (abs(offsetX) >= maxSwipePx * SWIPE_THRESHOLD && !hasTriggeredHaptic) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hasTriggeredHaptic = true
                            }
                        }
                    }
                )
            }
    ) {
        content()
    }
}
