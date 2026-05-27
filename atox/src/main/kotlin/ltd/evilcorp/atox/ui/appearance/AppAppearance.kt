// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.appearance

import androidx.appcompat.app.AppCompatDelegate

data class AppAppearance(
    val themeMode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val accentColorSeed: Int = 0xFF3F51B5.toInt(),
    val localeTag: String = ""
)
