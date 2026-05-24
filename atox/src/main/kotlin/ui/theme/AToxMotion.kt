package ltd.evilcorp.atox.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin

object AToxMotion {
    // Material 3 Emphasized Cubic Bezier curves
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    // Call screen motion spec tokens
    const val CallPulseDurationMs = 2000
    const val CallButtonToggleDurationMs = 300
    val CallPulseEasing = FastOutSlowInEasing

    // Screen transitions (Shared Axis Z)
    private const val DurationScreenTransition = 400
    private const val DurationFadeOut = 120
    private const val DurationFadeIn = 280

    // Tab switching transitions (Fade Through)
    private const val DurationTabTransition = 250
    private const val DurationTabFadeOut = 83
    private const val DurationTabFadeIn = 167

    fun sharedAxisZEnter(forward: Boolean): EnterTransition =
        fadeIn(
            animationSpec = tween(
                durationMillis = DurationFadeIn,
                delayMillis = DurationFadeOut,
                easing = EmphasizedDecelerate,
            ),
        ) + scaleIn(
            initialScale = if (forward) 0.92f else 1.08f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(
                durationMillis = DurationScreenTransition,
                easing = Emphasized,
            ),
        )

    fun sharedAxisZExit(forward: Boolean): ExitTransition =
        fadeOut(
            animationSpec = tween(
                durationMillis = DurationFadeOut,
                easing = EmphasizedAccelerate,
            ),
        ) + scaleOut(
            targetScale = if (forward) 1.08f else 0.92f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(
                durationMillis = DurationScreenTransition,
                easing = Emphasized,
            ),
        )

    fun fadeThroughEnter(): EnterTransition =
        fadeIn(
            animationSpec = tween(
                durationMillis = DurationTabFadeIn,
                delayMillis = DurationTabFadeOut,
                easing = LinearEasing,
            ),
        ) + scaleIn(
            initialScale = 0.96f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(
                durationMillis = DurationTabTransition,
                easing = Emphasized,
            ),
        )

    fun fadeThroughExit(): ExitTransition =
        fadeOut(
            animationSpec = tween(
                durationMillis = DurationTabFadeOut,
                easing = LinearEasing,
            ),
        ) + scaleOut(
            targetScale = 0.96f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(
                durationMillis = DurationTabTransition,
                easing = Emphasized,
            ),
        )

    fun fadeEnter(): EnterTransition =
        fadeIn(
            animationSpec = tween(150, easing = LinearEasing),
        ) + scaleIn(
            initialScale = 0.92f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(150, easing = Emphasized),
        )

    fun fadeExit(): ExitTransition =
        fadeOut(
            animationSpec = tween(75, easing = LinearEasing),
        ) + scaleOut(
            targetScale = 0.92f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = tween(75, easing = Emphasized),
        )

    fun sharedAxisXEnter(forward: Boolean): EnterTransition =
        fadeIn(
            animationSpec = tween(
                durationMillis = DurationFadeIn,
                delayMillis = DurationFadeOut,
                easing = EmphasizedDecelerate,
            ),
        ) + slideInHorizontally(
            initialOffsetX = { if (forward) 90 else -90 },
            animationSpec = tween(DurationScreenTransition, easing = Emphasized),
        )

    fun sharedAxisXExit(forward: Boolean): ExitTransition =
        fadeOut(
            animationSpec = tween(
                durationMillis = DurationFadeOut,
                easing = EmphasizedAccelerate,
            ),
        ) + slideOutHorizontally(
            targetOffsetX = { if (forward) -90 else 90 },
            animationSpec = tween(DurationScreenTransition, easing = Emphasized),
        )

    fun slideUpEnter(): EnterTransition =
        slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(
                durationMillis = DurationScreenTransition,
                easing = Emphasized,
            ),
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = DurationFadeIn,
                delayMillis = DurationFadeOut,
                easing = EmphasizedDecelerate,
            ),
        )

    fun slideDownExit(): ExitTransition =
        slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(
                durationMillis = DurationScreenTransition,
                easing = Emphasized,
            ),
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = DurationFadeOut,
                easing = EmphasizedAccelerate,
            ),
        )
}
