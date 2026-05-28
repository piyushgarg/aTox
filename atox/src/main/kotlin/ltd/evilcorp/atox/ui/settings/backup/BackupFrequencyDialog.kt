// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.settings.model.BackupFrequency

@Composable
fun BackupFrequencyDialog(
    backupFrequency: BackupFrequency,
    onDismissRequest: () -> Unit,
    onBackupFrequencyChanged: (BackupFrequency) -> Unit,
) {
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
        onDismissRequest = onDismissRequest,
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
                                onDismissRequest()
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
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun backupFrequencyTitle(frequency: BackupFrequency): String = when (frequency) {
    BackupFrequency.Off -> stringResource(R.string.backup_frequency_off)
    BackupFrequency.Daily -> stringResource(R.string.backup_frequency_daily)
    BackupFrequency.Weekly -> stringResource(R.string.backup_frequency_weekly)
    BackupFrequency.Monthly -> stringResource(R.string.backup_frequency_monthly)
}
