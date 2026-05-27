@file:SuppressLint("RestrictedApi")

package ltd.evilcorp.atox.ui.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.utilities.DynamicColor
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeTonalSpot

val LocalAToxThemeIsDark = staticCompositionLocalOf { false }

private val materialDynamicColors = MaterialDynamicColors()

@Composable
fun AToxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accentColorSeedArgb: Int = 0xFF3F51B5.toInt(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> seededColorScheme(accentColorSeedArgb, darkTheme)
    }

    CompositionLocalProvider(LocalAToxThemeIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

fun seededColorScheme(seedArgb: Int, darkTheme: Boolean): ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(seedArgb), darkTheme, 0.0)

    return ColorScheme(
        primary = materialDynamicColors.primary().asComposeColor(scheme),
        onPrimary = materialDynamicColors.onPrimary().asComposeColor(scheme),
        primaryContainer = materialDynamicColors.primaryContainer().asComposeColor(scheme),
        onPrimaryContainer = materialDynamicColors.onPrimaryContainer().asComposeColor(scheme),
        inversePrimary = materialDynamicColors.inversePrimary().asComposeColor(scheme),
        secondary = materialDynamicColors.secondary().asComposeColor(scheme),
        onSecondary = materialDynamicColors.onSecondary().asComposeColor(scheme),
        secondaryContainer = materialDynamicColors.secondaryContainer().asComposeColor(scheme),
        onSecondaryContainer = materialDynamicColors.onSecondaryContainer().asComposeColor(scheme),
        tertiary = materialDynamicColors.tertiary().asComposeColor(scheme),
        onTertiary = materialDynamicColors.onTertiary().asComposeColor(scheme),
        tertiaryContainer = materialDynamicColors.tertiaryContainer().asComposeColor(scheme),
        onTertiaryContainer = materialDynamicColors.onTertiaryContainer().asComposeColor(scheme),
        background = materialDynamicColors.background().asComposeColor(scheme),
        onBackground = materialDynamicColors.onBackground().asComposeColor(scheme),
        surface = materialDynamicColors.surface().asComposeColor(scheme),
        onSurface = materialDynamicColors.onSurface().asComposeColor(scheme),
        surfaceVariant = materialDynamicColors.surfaceVariant().asComposeColor(scheme),
        onSurfaceVariant = materialDynamicColors.onSurfaceVariant().asComposeColor(scheme),
        surfaceTint = materialDynamicColors.surfaceTint().asComposeColor(scheme),
        inverseSurface = materialDynamicColors.inverseSurface().asComposeColor(scheme),
        inverseOnSurface = materialDynamicColors.inverseOnSurface().asComposeColor(scheme),
        error = materialDynamicColors.error().asComposeColor(scheme),
        onError = materialDynamicColors.onError().asComposeColor(scheme),
        errorContainer = materialDynamicColors.errorContainer().asComposeColor(scheme),
        onErrorContainer = materialDynamicColors.onErrorContainer().asComposeColor(scheme),
        outline = materialDynamicColors.outline().asComposeColor(scheme),
        outlineVariant = materialDynamicColors.outlineVariant().asComposeColor(scheme),
        scrim = materialDynamicColors.scrim().asComposeColor(scheme),
        surfaceBright = materialDynamicColors.surfaceBright().asComposeColor(scheme),
        surfaceDim = materialDynamicColors.surfaceDim().asComposeColor(scheme),
        surfaceContainer = materialDynamicColors.surfaceContainer().asComposeColor(scheme),
        surfaceContainerHigh = materialDynamicColors.surfaceContainerHigh().asComposeColor(scheme),
        surfaceContainerHighest = materialDynamicColors.surfaceContainerHighest().asComposeColor(scheme),
        surfaceContainerLow = materialDynamicColors.surfaceContainerLow().asComposeColor(scheme),
        surfaceContainerLowest = materialDynamicColors.surfaceContainerLowest().asComposeColor(scheme)
    )
}

fun accentPreviewColor(seedArgb: Int, darkTheme: Boolean): Color =
    seededColorScheme(seedArgb, darkTheme).primary

fun accentPreviewContentColor(seedArgb: Int, darkTheme: Boolean): Color =
    seededColorScheme(seedArgb, darkTheme).onPrimary

fun avatarContentColor(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White

private fun DynamicColor.asComposeColor(scheme: DynamicScheme): Color = Color(getArgb(scheme))
