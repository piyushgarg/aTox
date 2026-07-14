// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.screens

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import ltd.evilcorp.atox.ui.settings.common.SettingsClickableRow
import ltd.evilcorp.atox.ui.settings.common.SettingsGroup
import ltd.evilcorp.atox.ui.settings.common.SettingsSwitchRow
import ltd.evilcorp.atox.ui.theme.AccentPresets
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private val ContentPaddingTop = 16.dp
private val ContentPaddingBottomDefault = 32.dp
private val HorizontalMargin = 16.dp
private val SpacingSpacedBy = 16.dp

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
    val bottomPadding = ltd.evilcorp.atox.ui.navigation.LocalTabPadding.current.calculateBottomPadding()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = HorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(SpacingSpacedBy),
        contentPadding = PaddingValues(
            top = ContentPaddingTop,
            bottom = ContentPaddingBottomDefault + bottomPadding
        )
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
                        val activePreset = androidx.compose.runtime.remember(currentAccentSeed) {
                            AccentPresets.find { it.seed.toArgb() == currentAccentSeed } ?: AccentPresets[0]
                        }
                        SettingsClickableRow(
                            title = stringResource(R.string.accent_color),
                            subtitle = activePreset.name
                        ) {
                            performHaptic()
                            onAccentColorClick()
                        }
                    }
                } else {
                    val activePreset = androidx.compose.runtime.remember(currentAccentSeed) {
                        AccentPresets.find { it.seed.toArgb() == currentAccentSeed } ?: AccentPresets[0]
                    }
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
