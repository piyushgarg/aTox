package ltd.evilcorp.atox.appearance

import androidx.appcompat.app.AppCompatDelegate

data class AppAppearance(
    val themeMode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val accentColorSeed: Int = 0xFF3F51B5.toInt(),
    val localeTag: String = ""
)
