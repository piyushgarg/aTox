// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

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
            tween(durationMillis = 350, easing = FastOutSlowInEasing)
        },
        label = "rotation"
    ) { back ->
        if (back) -180f else 0f
    }

    IconButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            rotationZ = rotation
        }
    ) {
        transition.AnimatedContent(
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
            }
        ) { back ->
            if (back) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.graphicsLayer { rotationZ = 180f }
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
