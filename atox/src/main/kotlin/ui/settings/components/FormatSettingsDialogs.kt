package ltd.evilcorp.atox.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import ltd.evilcorp.core.model.DateFormatPreference
import ltd.evilcorp.core.model.TimeFormatPreference

@Composable
fun DateFormatSettingsDialog(
    currentFormat: DateFormatPreference,
    onSelectFormat: (DateFormatPreference) -> Unit,
    onDismiss: () -> Unit,
    performHaptic: () -> Unit
) {
    val dateFormats = listOf(
        DateFormatPreference.System to stringResource(R.string.settings_date_format_system),
        DateFormatPreference.DMY to stringResource(R.string.settings_date_format_dmy),
        DateFormatPreference.DMYDots to stringResource(R.string.settings_date_format_dmy_dots),
        DateFormatPreference.MDY to stringResource(R.string.settings_date_format_mdy),
        DateFormatPreference.YMD to stringResource(R.string.settings_date_format_ymd)
    )
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_date_format_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dateFormats.forEach { item ->
                    val isSelected = item.first == currentFormat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                performHaptic()
                                onSelectFormat(item.first)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.second,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
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

@Composable
fun TimeFormatSettingsDialog(
    currentFormat: TimeFormatPreference,
    onSelectFormat: (TimeFormatPreference) -> Unit,
    onDismiss: () -> Unit,
    performHaptic: () -> Unit
) {
    val timeFormats = listOf(
        TimeFormatPreference.System to stringResource(R.string.settings_time_format_system),
        TimeFormatPreference.Hours24 to stringResource(R.string.settings_time_format_24h),
        TimeFormatPreference.Hours12 to stringResource(R.string.settings_time_format_12h)
    )
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_time_format_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                timeFormats.forEach { item ->
                    val isSelected = item.first == currentFormat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                performHaptic()
                                onSelectFormat(item.first)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.second,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
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
