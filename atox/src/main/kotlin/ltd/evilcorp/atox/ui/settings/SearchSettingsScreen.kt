// SPDX-FileCopyrightText: 2026 aTox contributors
// SPDX-License-Identifier: GPL-3.0-only
package ltd.evilcorp.atox.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.settings.common.SearchableSetting
import ltd.evilcorp.atox.ui.settings.common.SettingsSearchPopup
import ltd.evilcorp.atox.ui.settings.common.SettingsSearchIndex

@Suppress("FunctionNaming", "LongMethod")
@Composable
internal fun SearchSettingsScreen(
    settings: Settings,
    appearance: AppAppearance,
    onItemClick: (SearchableSetting) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val storedSettings by settings.state.collectAsState()

    val performHaptic = {
        if (storedSettings.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val currentLanguageCode = remember(appearance.localeTag) {
        appearance.localeTag.substringBefore('-').substringBefore(',')
    }
    val systemDefaultLabel = stringResource(R.string.pref_theme_follow_system)
    val languages = remember(systemDefaultLabel) {
        listOf(
            "" to systemDefaultLabel,
            "en" to "English",
            "ru" to "Русский",
            "sv" to "Svenska",
            "de" to "Deutsch",
            "es" to "Español",
            "fr" to "Français",
            "it" to "Italiano",
            "uk" to "Українська"
        )
    }

    val searchItems = remember(languages, currentLanguageCode, appearance.themeMode) {
        SettingsSearchIndex.buildSearchIndex(
            context,
            languages,
            currentLanguageCode,
            appearance.themeMode,
            viewModel
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        SettingsSearchPopup(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            searchItems = searchItems,
            onDismissRequest = onBack,
            performHaptic = performHaptic,
            onItemClick = onItemClick
        )
    }
}
