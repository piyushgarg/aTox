package ltd.evilcorp.atox.ui.theme

import androidx.compose.ui.graphics.Color

// Offline / Status Colors
val StatusAvailable = Color(0xFF66BB6A)
val StatusAway = Color(0xFFFFB74D)
val StatusBusy = Color(0xFFEF5350)
val StatusOffline = Color(0xFF757680)

// Preset source colors used to generate full Material 3 color schemes.
data class AccentPreset(val name: String, val seed: Color)

val AccentPresets = listOf(
    AccentPreset(
        name = "Ocean Indigo",
        seed = Color(0xFF3F51B5)
    ),
    AccentPreset(
        name = "Emerald Forest",
        seed = Color(0xFF00796B)
    ),
    AccentPreset(
        name = "Coral Sunset",
        seed = Color(0xFFE64A19)
    ),
    AccentPreset(
        name = "Sakura Pink",
        seed = Color(0xFFD81B60)
    ),
    AccentPreset(
        name = "Lavender Dream",
        seed = Color(0xFF7B1FA2)
    )
)

// Modern Kotlin-defined contact avatar placeholder backgrounds
val ContactBackgrounds = listOf(
    Color(0xFFEC407A),
    Color(0xFFFF7043),
    Color(0xFFFFA726),
    Color(0xFFFFEE58),
    Color(0xFF66BB6A),
    Color(0xFF26A69A),
    Color(0xFF29B6F6),
    Color(0xFFAB47BC)
)

val ContactBackgroundsInts = intArrayOf(
    0xFFEC407A.toInt(),
    0xFFFF7043.toInt(),
    0xFFFFA726.toInt(),
    0xFFFFEE58.toInt(),
    0xFF66BB6A.toInt(),
    0xFF26A69A.toInt(),
    0xFF29B6F6.toInt(),
    0xFFAB47BC.toInt()
)
