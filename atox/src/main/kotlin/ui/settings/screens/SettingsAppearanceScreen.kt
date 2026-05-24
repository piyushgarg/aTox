// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.screens

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.settings.components.SettingsClickableRow
import ltd.evilcorp.atox.ui.settings.components.SettingsGroup
import ltd.evilcorp.atox.ui.settings.components.SettingsSwitchRow
import ltd.evilcorp.atox.ui.theme.AccentPresets
import ltd.evilcorp.core.model.DateFormatPreference
import ltd.evilcorp.core.model.TimeFormatPreference
import androidx.compose.ui.graphics.toArgb

@Composable
fun SettingsAppearanceScreen(
    paddingValues: PaddingValues,
    currentLanguageCode: String,
    languages: List<Pair<String, String>>,
    appThemeMode: Int,
    timeFormatPreference: TimeFormatPreference,
    dateFormatPreference: DateFormatPreference,
    dynamicColor: Boolean,
    currentAccentSeed: Int,
    hapticEnabled: Boolean,
    performHaptic: () -> Unit,
    onLanguageClick: () -> Unit,
    onThemeClick: () -> Unit,
    onDateFormatClick: () -> Unit,
    onTimeFormatClick: () -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onAccentColorClick: () -> Unit,
    onHapticEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            SettingsGroup(title = stringResource(R.string.language_and_localization)) {
                SettingsClickableRow(
                    title = stringResource(R.string.language),
                    subtitle = languages.find { it.first == currentLanguageCode }?.second ?: "English"
                ) {
                    performHaptic()
                    onLanguageClick()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                val dateFormatLabel = when (dateFormatPreference) {
                    DateFormatPreference.System -> stringResource(R.string.settings_date_format_system)
                    DateFormatPreference.DMY -> stringResource(R.string.settings_date_format_dmy)
                    DateFormatPreference.DMYDots -> stringResource(R.string.settings_date_format_dmy_dots)
                    DateFormatPreference.MDY -> stringResource(R.string.settings_date_format_mdy)
                    DateFormatPreference.YMD -> stringResource(R.string.settings_date_format_ymd)
                }
                SettingsClickableRow(
                    title = stringResource(R.string.settings_date_format_title),
                    subtitle = dateFormatLabel
                ) {
                    performHaptic()
                    onDateFormatClick()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                val timeFormatLabel = when (timeFormatPreference) {
                    TimeFormatPreference.System -> stringResource(R.string.settings_time_format_system)
                    TimeFormatPreference.Hours24 -> stringResource(R.string.settings_time_format_24h)
                    TimeFormatPreference.Hours12 -> stringResource(R.string.settings_time_format_12h)
                }
                SettingsClickableRow(
                    title = stringResource(R.string.settings_time_format_title),
                    subtitle = timeFormatLabel
                ) {
                    performHaptic()
                    onTimeFormatClick()
                }
            }
        }

        item {
            SettingsGroup(title = stringResource(R.string.pref_heading_theme)) {
                val themeLabel = when (appThemeMode) {
                    AppCompatDelegate.MODE_NIGHT_YES -> stringResource(R.string.pref_theme_dark)
                    AppCompatDelegate.MODE_NIGHT_NO -> stringResource(R.string.pref_theme_light)
                    else -> stringResource(R.string.pref_theme_follow_system)
                }
                SettingsClickableRow(
                    title = stringResource(R.string.pref_heading_theme),
                    subtitle = themeLabel
                ) {
                    performHaptic()
                    onThemeClick()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.dynamic_theme),
                        subtitle = stringResource(R.string.settings_dynamic_theme_subtitle),
                        checked = dynamicColor
                    ) { checked ->
                        performHaptic()
                        onDynamicColorChanged(checked)
                    }
                    if (!dynamicColor) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        val activePreset = AccentPresets.find { it.seed.toArgb() == currentAccentSeed } ?: AccentPresets[0]
                        SettingsClickableRow(
                            title = stringResource(R.string.accent_color),
                            subtitle = activePreset.name
                        ) {
                            performHaptic()
                            onAccentColorClick()
                        }
                    }
                } else {
                    val activePreset = AccentPresets.find { it.seed.toArgb() == currentAccentSeed } ?: AccentPresets[0]
                    SettingsClickableRow(
                        title = stringResource(R.string.accent_color),
                        subtitle = activePreset.name
                    ) {
                        performHaptic()
                        onAccentColorClick()
                    }
                }
            }
        }

        item {
            SettingsGroup(title = stringResource(R.string.interaction_vibration)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_haptic_title),
                    subtitle = stringResource(R.string.pref_haptic_summary),
                    checked = hapticEnabled
                ) { checked ->
                    onHapticEnabledChanged(checked)
                    performHaptic()
                }
            }
        }
    }
}
