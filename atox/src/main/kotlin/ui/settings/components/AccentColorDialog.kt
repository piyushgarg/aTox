package ltd.evilcorp.atox.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.theme.AccentPresets
import ltd.evilcorp.atox.ui.theme.LocalAToxThemeIsDark
import ltd.evilcorp.atox.ui.theme.accentPreviewColor

@Composable
fun AccentColorDialog(
    currentAccentSeed: Int,
    onAccentColorSeedChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isDarkTheme = LocalAToxThemeIsDark.current

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accent_preset), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Row 1: first 3 presets
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 0..2) {
                        val preset = AccentPresets[i]
                        val isSelected = preset.seed.toArgb() == currentAccentSeed
                        val previewColor = remember(preset.seed, isDarkTheme) {
                            accentPreviewColor(preset.seed.toArgb(), isDarkTheme)
                        }
                        ColorSwatch(
                            name = preset.name,
                            previewColor = previewColor,
                            isSelected = isSelected,
                            onClick = {
                                onAccentColorSeedChanged(preset.seed.toArgb())
                                onDismiss()
                            }
                        )
                    }
                }
                // Row 2: next 2 presets
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 3..4) {
                        val preset = AccentPresets[i]
                        val isSelected = preset.seed.toArgb() == currentAccentSeed
                        val previewColor = remember(preset.seed, isDarkTheme) {
                            accentPreviewColor(preset.seed.toArgb(), isDarkTheme)
                        }
                        ColorSwatch(
                            name = preset.name,
                            previewColor = previewColor,
                            isSelected = isSelected,
                            onClick = {
                                onAccentColorSeedChanged(preset.seed.toArgb())
                                onDismiss()
                            }
                        )
                    }
                    // Placeholder box to balance the Row layout
                    Box(modifier = Modifier.width(72.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun ColorSwatch(
    name: String,
    previewColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(previewColor)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}
