// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.settings.formatSize
import ltd.evilcorp.atox.ui.settings.common.SettingsGroup
import ltd.evilcorp.atox.ui.settings.common.SettingsClickableRow
import ltd.evilcorp.atox.ui.settings.common.SettingsSwitchRow
import ltd.evilcorp.atox.ui.settings.backup.BackupFrequencyDialog
import ltd.evilcorp.domain.features.settings.model.BackupDestination
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider

private val ContentPaddingTop = 16.dp
private val ContentPaddingBottomDefault = 32.dp
private val HorizontalMargin = 16.dp
private val SpacingSpacedBy = 16.dp
private const val BYTES_IN_KB = 1024L

@Suppress("FunctionNaming", "UnstableCollections", "UNUSED_PARAMETER", "MagicNumber")
@Composable
fun BackupSettingsScreen(
    paddingValues: PaddingValues,
    backupExporting: Boolean,
    backupImporting: Boolean,
    backupFrequency: BackupFrequency,
    backupUseCellular: Boolean,
    backupGoogleAccount: String,
    lastLocalBackupTimeMs: Long,
    lastLocalBackupSizeKb: Long,
    lastGoogleBackupTimeMs: Long,
    lastGoogleBackupSizeKb: Long,
    onBackupFrequencyChanged: (BackupFrequency) -> Unit,
    onBackupUseCellularChanged: (Boolean) -> Unit,
    onGoogleAccountClick: () -> Unit,
    onCreateBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    performHaptic: () -> Unit,
    modifier: Modifier = Modifier,
    backupProviders: List<IBackupDataProvider> = emptyList(),
    automaticBackupEnabled: Boolean = false,
    backupDestinations: Set<BackupDestination> = emptySet(),
    selectedBackupIds: Set<String> = emptySet(),
    mandatoryBackupId: String = "",
    onAutomaticBackupEnabledChanged: (Boolean) -> Unit = {},
    onBackupDestinationsChanged: (Set<BackupDestination>) -> Unit = {},
    onSelectedBackupIdsChanged: (Set<String>) -> Unit = {},
    onCreateGoogleBackupClick: () -> Unit = {},
    onRestoreGoogleBackupClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showBackupFrequencyDialog by remember { mutableStateOf(false) }

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
        // 1. Last Backup (Material 3 Card)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.backup_last_backup_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.backup_last_backup_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val neverStr = stringResource(R.string.backup_frequency_off)
                    val localText = if (lastLocalBackupTimeMs > 0) {
                        val dateStr = android.text.format.DateFormat.getMediumDateFormat(context).format(lastLocalBackupTimeMs)
                        val timeStr = android.text.format.DateFormat.getTimeFormat(context).format(lastLocalBackupTimeMs)
                        val sizeStr = formatSize(context, lastLocalBackupSizeKb * BYTES_IN_KB)
                        "$dateStr $timeStr • $sizeStr"
                    } else neverStr

                    val googleText = if (lastGoogleBackupTimeMs > 0) {
                        val dateStr = android.text.format.DateFormat.getMediumDateFormat(context).format(lastGoogleBackupTimeMs)
                        val timeStr = android.text.format.DateFormat.getTimeFormat(context).format(lastGoogleBackupTimeMs)
                        val sizeStr = formatSize(context, lastGoogleBackupSizeKb * BYTES_IN_KB)
                        "$dateStr $timeStr • $sizeStr"
                    } else neverStr

                    Column(
                        modifier = Modifier.padding(start = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.backup_local_label, localText),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.backup_google_label, googleText),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            performHaptic()
                            onCreateBackupClick()
                        },
                        enabled = !backupExporting,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .padding(start = 48.dp, top = 4.dp)
                    ) {
                        Text(
                            text = if (backupExporting) stringResource(R.string.backup_creating)
                            else stringResource(R.string.backup_btn_backup),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // 2. Google Drive Settings (Material 3 Card list)
        item {
            SettingsGroup(title = stringResource(R.string.backup_google_settings_group)) {
                val freqSubtitle = when (backupFrequency) {
                    BackupFrequency.Daily -> stringResource(R.string.backup_frequency_daily)
                    BackupFrequency.Weekly -> stringResource(R.string.backup_frequency_weekly)
                    BackupFrequency.Monthly -> stringResource(R.string.backup_frequency_monthly)
                    BackupFrequency.Off -> stringResource(R.string.backup_frequency_off)
                }
                SettingsClickableRow(
                    title = stringResource(R.string.backup_frequency_label),
                    subtitle = freqSubtitle,
                    showArrow = false
                ) {
                    performHaptic()
                    showBackupFrequencyDialog = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsClickableRow(
                    title = stringResource(R.string.backup_google_account),
                    subtitle = backupGoogleAccount.ifBlank {
                        stringResource(R.string.backup_google_account_not_selected)
                    },
                    showArrow = false
                ) {
                    performHaptic()
                    onGoogleAccountClick()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsSwitchRow(
                    title = stringResource(R.string.backup_use_cellular),
                    subtitle = stringResource(R.string.backup_use_cellular_description),
                    checked = backupUseCellular,
                    onCheckedChange = onBackupUseCellularChanged
                )
            }
        }

        // 3. Advanced Restore Options (Material 3 Card list)
        item {
            SettingsGroup(title = stringResource(R.string.backup_restore_group)) {
                SettingsClickableRow(
                    title = stringResource(R.string.backup_restore_from_file_row),
                    subtitle = if (backupImporting) stringResource(R.string.backup_restoring) else "",
                    showArrow = false
                ) {
                    if (!backupImporting) {
                        performHaptic()
                        onRestoreBackupClick()
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsClickableRow(
                    title = stringResource(R.string.backup_restore_from_google_drive),
                    subtitle = if (backupImporting) stringResource(R.string.backup_restoring) else "",
                    showArrow = false
                ) {
                    if (!backupImporting) {
                        performHaptic()
                        onRestoreGoogleBackupClick()
                    }
                }
            }
        }
    }

    if (showBackupFrequencyDialog) {
        BackupFrequencyDialog(
            backupFrequency = backupFrequency,
            onDismissRequest = { showBackupFrequencyDialog = false },
            onBackupFrequencyChanged = onBackupFrequencyChanged
        )
    }
}
