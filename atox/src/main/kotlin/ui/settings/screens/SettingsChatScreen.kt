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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.settings.common.SettingsClickableRow
import ltd.evilcorp.atox.ui.settings.common.SettingsGroup
import ltd.evilcorp.atox.ui.settings.common.SettingsSwitchRow
import ltd.evilcorp.atox.ui.settings.common.SettingsErrorClickableRow
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
    var showConfirmDialog by remember { mutableStateOf(false) }

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
                SettingsErrorClickableRow(
                    title = stringResource(R.string.settings_clear_cache_title),
                    subtitle = stringResource(R.string.settings_clear_cache_subtitle, cacheSizeText)
                ) {
                    performHaptic()
                    showConfirmDialog = true
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.settings_clear_cache_title), fontWeight = FontWeight.Bold) },
            text = { Text("Вы уверены, что хотите удалить кэш файлов? Все скачанные медиафайлы останутся на устройстве, но будут удалены из кэша приложения.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onClearCacheClick()
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.reject))
                }
            }
        )
    }
}
