// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.common

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.settings.SettingsViewModel

internal enum class SettingsDestination {
    Root,
    Appearance,
    Chat,
    Sounds,
    Connection,
    Backup,
    Language,
    Theme,
}

internal data class SearchableSetting(
    val title: String,
    val subtitle: String,
    val destination: SettingsDestination,
    val category: String,
    val onTrigger: (() -> Unit)? = null
)

internal object SettingsSearchIndex {
    fun buildSearchIndex(
        context: Context,
        languages: List<Pair<String, String>>,
        currentLanguageCode: String,
        appThemeMode: Int,
        viewModel: SettingsViewModel
    ): List<SearchableSetting> {
        return listOf(
            SearchableSetting(
                title = context.getString(R.string.language),
                subtitle = languages.find { it.first == currentLanguageCode }?.second ?: "English",
                destination = SettingsDestination.Language,
                category = context.getString(R.string.appearance_and_design)
            ),
            SearchableSetting(
                title = context.getString(R.string.pref_heading_theme),
                subtitle = when (appThemeMode) {
                    AppCompatDelegate.MODE_NIGHT_YES -> context.getString(R.string.pref_theme_dark)
                    AppCompatDelegate.MODE_NIGHT_NO -> context.getString(R.string.pref_theme_light)
                    else -> context.getString(R.string.pref_theme_follow_system)
                },
                destination = SettingsDestination.Theme,
                category = context.getString(R.string.appearance_and_design)
            ),
            SearchableSetting(
                title = context.getString(R.string.settings_sounds_group),
                subtitle = context.getString(R.string.settings_sounds_summary),
                destination = SettingsDestination.Sounds,
                category = context.getString(R.string.settings_sounds_group)
            ),
            SearchableSetting(
                title = context.getString(R.string.backup_title),
                subtitle = context.getString(R.string.backup_settings_subtitle),
                destination = SettingsDestination.Backup,
                category = context.getString(R.string.settings_privacy_group)
            ),
            SearchableSetting(
                title = context.getString(R.string.settings_clear_cache_title),
                subtitle = context.getString(R.string.settings_clear_cache_title),
                destination = SettingsDestination.Root,
                category = context.getString(R.string.settings_storage_group)
            ),
            SearchableSetting(
                title = context.getString(R.string.settings_proxy_type),
                subtitle = context.getString(R.string.settings_proxy_type),
                destination = SettingsDestination.Root,
                category = context.getString(R.string.settings_proxy_group),
                onTrigger = { viewModel.setShowProxyDialog(true) }
            )
        )
    }
}
