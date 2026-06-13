// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import ltd.evilcorp.atox.ui.common.AtoxConfirmDialog
import ltd.evilcorp.atox.ui.settings.common.SettingsClickableRow
import ltd.evilcorp.atox.ui.settings.common.SettingsGroup
import ltd.evilcorp.atox.ui.settings.common.SettingsSwitchRow
import ltd.evilcorp.domain.features.settings.model.FtAutoAccept

private val ContentPaddingTop = 16.dp
private val ContentPaddingBottomDefault = 32.dp
private val HorizontalMargin = 16.dp
private val SpacingSpacedBy = 16.dp
private val CardPadding = 16.dp
private val CardCornerRadius = 16.dp
private val ButtonCornerRadius = 12.dp
private val SpacerHeightSmall = 8.dp
private val SpacerHeightMedium = 16.dp
private const val ERROR_CONTAINER_ALPHA = 0.15f

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
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(CardCornerRadius),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = ERROR_CONTAINER_ALPHA)
                )
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(CardPadding),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.settings_clear_cache_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Start)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(SpacerHeightSmall))
                    Text(
                        text = stringResource(R.string.settings_clear_cache_subtitle, cacheSizeText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Start)
                    )

                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(SpacerHeightMedium))

                    androidx.compose.material3.Button(
                        onClick = {
                            performHaptic()
                            showConfirmDialog = true
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(ButtonCornerRadius)
                    ) {
                        Text(stringResource(R.string.settings_clear_cache_title), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AtoxConfirmDialog(
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                showConfirmDialog = false
                onClearCacheClick()
            },
            title = stringResource(R.string.settings_clear_cache_title),
            text = "Are you sure you want to clear the file cache? " +
                "All downloaded media files will remain on your device, " +
                "but they will be removed from the app's cache.",
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.reject),
            isDangerous = true
        )
    }
}
