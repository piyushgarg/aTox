// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private const val DURATION_ROTATION_MS = 350
private const val DURATION_FADE_IN_MS = 200
private const val DURATION_FADE_OUT_MS = 150
private const val ANGLE_ROTATION_BACK = -180f
private const val ANGLE_ROTATION_ICON = 180f
private const val ANGLE_ROTATION_DEFAULT = 0f

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MorphingNavigationIcon(
    isBack: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = isBack, label = "morphIconTransition")
    val rotation by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = DURATION_ROTATION_MS, easing = FastOutSlowInEasing)
        },
        label = "rotation"
    ) { back ->
        if (back) ANGLE_ROTATION_BACK else ANGLE_ROTATION_DEFAULT
    }

    IconButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            rotationZ = rotation
        }
    ) {
        transition.AnimatedContent(
            transitionSpec = {
                fadeIn(animationSpec = tween(DURATION_FADE_IN_MS)) togetherWith fadeOut(animationSpec = tween(DURATION_FADE_OUT_MS))
            }
        ) { back ->
            if (back) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.graphicsLayer { rotationZ = ANGLE_ROTATION_ICON }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
        }
    }
}
