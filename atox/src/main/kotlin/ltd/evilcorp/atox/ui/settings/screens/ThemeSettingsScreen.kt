// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.SettingsRadioGroupCard

@Suppress("FunctionNaming")
@Composable
fun ThemeSettingsScreen(
    paddingValues: PaddingValues,
    appThemeMode: Int,
    onThemeSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val themes = listOf(
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM to stringResource(R.string.pref_theme_follow_system),
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO to stringResource(R.string.pref_theme_light),
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES to stringResource(R.string.pref_theme_dark)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
    ) {
        item {
            SettingsRadioGroupCard(
                options = themes,
                selectedOption = appThemeMode,
                onOptionSelect = onThemeSelect
            )
        }
    }
}
