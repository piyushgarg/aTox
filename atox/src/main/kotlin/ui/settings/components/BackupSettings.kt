// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.core.model.BackupDestination
import ltd.evilcorp.core.model.BackupFrequency
import ltd.evilcorp.domain.backup.BackupDataProvider

@Composable
fun BackupSettingsScreen(
    paddingValues: PaddingValues,
    backupProviders: List<BackupDataProvider>,
    backupExporting: Boolean,
    backupImporting: Boolean,
    backupPasswordEnabled: Boolean,
    backupPassword: String,
    backupPasswordVisible: Boolean,
    automaticBackupEnabled: Boolean,
    backupFrequency: BackupFrequency,
    backupUseCellular: Boolean,
    backupDestinations: Set<BackupDestination>,
    backupEndToEndEncryptionEnabled: Boolean,
    backupGoogleAccount: String,
    selectedBackupIds: Set<String>,
    mandatoryBackupId: String,
    onBackupPasswordEnabledChanged: (Boolean) -> Unit,
    onBackupPasswordChanged: (String) -> Unit,
    onBackupPasswordVisibleChanged: (Boolean) -> Unit,
    onAutomaticBackupEnabledChanged: (Boolean) -> Unit,
    onBackupFrequencyChanged: (BackupFrequency) -> Unit,
    onBackupUseCellularChanged: (Boolean) -> Unit,
    onBackupDestinationsChanged: (Set<BackupDestination>) -> Unit,
    onBackupEndToEndEncryptionEnabledChanged: (Boolean) -> Unit,
    onGoogleAccountClick: () -> Unit,
    onSelectedBackupIdsChanged: (Set<String>) -> Unit,
    onCreateBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    performHaptic: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showBackupFrequencyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            SettingsGroup(title = stringResource(R.string.backup_security_group)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.backup_encryption_enabled),
                    subtitle = stringResource(R.string.backup_encryption_description),
                    checked = backupPasswordEnabled
                ) { checked ->
                    performHaptic()
                    onBackupPasswordEnabledChanged(checked)
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.backup_modules_group),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
        items(backupProviders.size) { index ->
            val provider = backupProviders[index]
            val mandatory = provider.id == mandatoryBackupId
            BackupModuleCard(
                title = stringResource(provider.displayNameRes),
                description = stringResource(provider.descriptionRes),
                checked = mandatory || provider.id in selectedBackupIds,
                enabled = !mandatory,
                onCheckedChange = { checked ->
                    val newIds = if (checked) {
                        selectedBackupIds + provider.id
                    } else {
                        selectedBackupIds - provider.id
                    }
                    onSelectedBackupIdsChanged(newIds)
                }
            )
        }
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                SettingsSwitchRow(
                    title = stringResource(R.string.backup_password_protect),
                    subtitle = stringResource(R.string.backup_password_description),
                    checked = backupPasswordEnabled
                ) { checked ->
                    performHaptic()
                    onBackupPasswordEnabledChanged(checked)
                }
                AnimatedVisibility(
                    visible = backupPasswordEnabled,
                    enter = AToxMotion.fadeEnter(),
                    exit = AToxMotion.fadeExit(),
                ) {
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = onBackupPasswordChanged,
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        visualTransformation = if (backupPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { onBackupPasswordVisibleChanged(!backupPasswordVisible) }) {
                                Icon(
                                    imageVector = if (backupPasswordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = if (backupPasswordVisible) {
                                        stringResource(R.string.hide)
                                    } else {
                                        stringResource(R.string.show)
                                    },
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    )
                }
            }
        }
        item {
            SettingsGroup(title = stringResource(R.string.backup_auto_group)) {
                SettingsClickableRow(
                    title = stringResource(R.string.backup_google_account),
                    subtitle = backupGoogleAccount.ifBlank {
                        stringResource(R.string.backup_google_account_not_selected)
                    }
                ) {
                    performHaptic()
                    onGoogleAccountClick()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                SettingsClickableRow(
                    title = stringResource(R.string.backup_manage_google_storage),
                    subtitle = stringResource(R.string.backup_manage_google_storage_description),
                ) {
                    performHaptic()
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://one.google.com/storage")))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                SettingsSwitchRow(
                    title = stringResource(R.string.backup_automatic_enabled),
                    subtitle = stringResource(R.string.backup_automatic_description),
                    checked = automaticBackupEnabled
                ) { checked ->
                    performHaptic()
                    onAutomaticBackupEnabledChanged(checked)
                }
                AnimatedVisibility(
                    visible = automaticBackupEnabled,
                    enter = AToxMotion.fadeEnter(),
                    exit = AToxMotion.fadeExit(),
                ) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        SettingsClickableRow(
                            title = stringResource(R.string.backup_frequency_title),
                            subtitle = backupFrequencyTitle(backupFrequency),
                        ) {
                            performHaptic()
                            showBackupFrequencyDialog = true
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        SettingsSwitchRow(
                            title = stringResource(R.string.backup_use_cellular),
                            subtitle = stringResource(R.string.backup_use_cellular_description),
                            checked = backupUseCellular,
                        ) { checked ->
                            performHaptic()
                            onBackupUseCellularChanged(checked)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        BackupDestination.entries.forEach { destination ->
                            val selected = destination in backupDestinations
                            SettingsSwitchRow(
                                title = backupDestinationTitle(destination),
                                subtitle = backupDestinationSubtitle(destination),
                                checked = selected,
                            ) { checked ->
                                performHaptic()
                                val current = backupDestinations
                                val nextDests = if (checked) {
                                    current + destination
                                } else {
                                    (current - destination).takeIf { it.isNotEmpty() }
                                        ?: setOf(BackupDestination.Local)
                                }
                                onBackupDestinationsChanged(nextDests)
                            }
                            if (destination != BackupDestination.entries.last()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }
        }
        item {
            SettingsGroup(title = stringResource(R.string.backup_e2e_group)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.backup_e2e_title),
                    subtitle = stringResource(R.string.backup_e2e_description),
                    checked = backupEndToEndEncryptionEnabled,
                ) { checked ->
                    performHaptic()
                    onBackupEndToEndEncryptionEnabledChanged(checked)
                }
            }
        }
        item {
            Button(
                onClick = {
                    performHaptic()
                    onCreateBackupClick()
                },
                enabled = !backupExporting && (!backupPasswordEnabled || backupPassword.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (backupExporting) {
                        stringResource(R.string.backup_creating)
                    } else {
                        stringResource(R.string.backup_create)
                    }
                )
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    performHaptic()
                    onRestoreBackupClick()
                },
                enabled = !backupImporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (backupImporting) {
                        stringResource(R.string.backup_restoring)
                    } else {
                        stringResource(R.string.backup_restore_from_file)
                    }
                )
            }
        }
    }

    if (showBackupFrequencyDialog) {
        val frequencies = listOf(
            BackupFrequency.Off,
            BackupFrequency.Daily,
            BackupFrequency.Weekly,
            BackupFrequency.Monthly,
        )
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { showBackupFrequencyDialog = false },
            title = { Text(stringResource(R.string.backup_frequency_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    frequencies.forEach { frequency ->
                        val isSelected = frequency == backupFrequency
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onBackupFrequencyChanged(frequency)
                                    showBackupFrequencyDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = backupFrequencyTitle(frequency),
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupFrequencyDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun BackupModuleCard(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun backupFrequencyTitle(frequency: BackupFrequency): String = when (frequency) {
    BackupFrequency.Off -> stringResource(R.string.backup_frequency_off)
    BackupFrequency.Daily -> stringResource(R.string.backup_frequency_daily)
    BackupFrequency.Weekly -> stringResource(R.string.backup_frequency_weekly)
    BackupFrequency.Monthly -> stringResource(R.string.backup_frequency_monthly)
}

@Composable
private fun backupDestinationTitle(destination: BackupDestination): String = when (destination) {
    BackupDestination.Local -> stringResource(R.string.backup_destination_local)
    BackupDestination.GoogleDrive -> stringResource(R.string.backup_destination_google)
    BackupDestination.Nextcloud -> stringResource(R.string.backup_destination_nextcloud)
    BackupDestination.WebDav -> stringResource(R.string.backup_destination_webdav)
}

@Composable
private fun backupDestinationSubtitle(destination: BackupDestination): String = when (destination) {
    BackupDestination.Local -> stringResource(R.string.backup_destination_local_description)
    BackupDestination.GoogleDrive -> stringResource(R.string.backup_destination_google_description)
    BackupDestination.Nextcloud -> stringResource(R.string.backup_destination_nextcloud_description)
    BackupDestination.WebDav -> stringResource(R.string.backup_destination_webdav_description)
}
