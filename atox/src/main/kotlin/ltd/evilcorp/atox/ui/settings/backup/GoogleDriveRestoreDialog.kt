package ltd.evilcorp.atox.ui.settings.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ltd.evilcorp.domain.features.backup.model.CloudBackupInfo
import androidx.compose.ui.res.stringResource
import ltd.evilcorp.atox.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val KB_IN_MB = 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveRestoreDialog(
    backups: List<CloudBackupInfo>,
    onBackupSelected: (CloudBackupInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_title)) },
        text = {
            if (backups.isEmpty()) {
                Text(stringResource(R.string.backup_none_found))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(backups) {
                        val locale = getLocaleFromConfiguration(androidx.compose.ui.platform.LocalConfiguration.current)
                        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", locale)
                        val dateStr = formatter.format(Date(it.createdTimeMs))
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBackupSelected(it) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.backup_size_format, it.sizeKb / KB_IN_MB, it.sizeKb),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = stringResource(R.string.backup_created_format, dateStr), style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

private fun getLocaleFromConfiguration(configuration: android.content.res.Configuration): Locale {
    return androidx.core.os.ConfigurationCompat.getLocales(configuration).get(0) ?: Locale.getDefault()
}
