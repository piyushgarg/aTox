// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.screens

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
import ltd.evilcorp.atox.ui.settings.components.SettingsClickableRow
import ltd.evilcorp.atox.ui.settings.components.SettingsGroup
import ltd.evilcorp.atox.ui.settings.components.SettingsSwitchRow
import ltd.evilcorp.core.model.FtAutoAccept

@Composable
fun SettingsChatScreen(
    paddingValues: PaddingValues,
    ftAutoAccept: FtAutoAccept,
    autoSaveToDownloads: Boolean,
    autoSaveDirectoryLabel: String,
    cacheSizeText: String,
    enableReplies: Boolean,
    performHaptic: () -> Unit,
    onFtAutoAcceptClick: () -> Unit,
    onAutoSaveToDownloadsChanged: (Boolean) -> Unit,
    onAutoSaveDirectoryClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onEnableRepliesChanged: (Boolean) -> Unit,
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
            SettingsGroup(title = stringResource(R.string.settings_ft_group)) {
                val ftLabel = when (ftAutoAccept) {
                    FtAutoAccept.None -> stringResource(R.string.pref_ft_auto_accept_none)
                    FtAutoAccept.Images -> stringResource(R.string.pref_ft_auto_accept_images)
                    FtAutoAccept.All -> stringResource(R.string.pref_ft_auto_accept_all)
                }
                SettingsClickableRow(
                    title = stringResource(R.string.pref_heading_ft_auto_accept),
                    subtitle = ftLabel
                ) {
                    performHaptic()
                    onFtAutoAcceptClick()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_auto_save_title),
                    subtitle = stringResource(R.string.settings_auto_save_subtitle),
                    checked = autoSaveToDownloads
                ) { checked ->
                    performHaptic()
                    onAutoSaveToDownloadsChanged(checked)
                }
                if (autoSaveToDownloads) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    SettingsClickableRow(
                        title = stringResource(R.string.settings_auto_save_directory_title),
                        subtitle = autoSaveDirectoryLabel
                    ) {
                        performHaptic()
                        onAutoSaveDirectoryClick()
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_enable_replies_title),
                    subtitle = stringResource(R.string.settings_enable_replies_subtitle),
                    checked = enableReplies
                ) { checked ->
                    performHaptic()
                    onEnableRepliesChanged(checked)
                }
            }
        }

        item {
            SettingsGroup(title = stringResource(R.string.settings_storage_group)) {
                SettingsClickableRow(
                    title = stringResource(R.string.settings_clear_cache_title),
                    subtitle = stringResource(R.string.settings_clear_cache_subtitle, cacheSizeText)
                ) {
                    performHaptic()
                    onClearCacheClick()
                }
            }
        }
    }
}
