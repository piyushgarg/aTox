// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R

private val ContentPaddingTop = 16.dp
private val ContentPaddingBottomDefault = 32.dp
private val HorizontalMargin = 16.dp
private val SpacingSpacedBy = 16.dp
private val CardCornerRadius = 24.dp
private val RowPaddingHorizontal = 16.dp
private val RowPaddingVertical = 20.dp
private val IconContainerSize = 40.dp
private val IconSize = 22.dp
private val ArrowSize = 20.dp

@Composable
fun SettingsRootContent(
    paddingValues: PaddingValues,
    currentLanguageLabel: String,
    themeLabel: String,
    onAppearanceClick: () -> Unit,
    onChatClick: () -> Unit,
    onSoundsClick: () -> Unit,
    onConnectionClick: () -> Unit,
    onBackupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsCategoryRow(
                        title = stringResource(R.string.appearance_and_design),
                        subtitle = "$currentLanguageLabel, $themeLabel",
                        icon = Icons.Default.Palette,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onAppearanceClick
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    SettingsCategoryRow(
                        title = stringResource(R.string.settings_ft_group),
                        subtitle = stringResource(R.string.settings_auto_save_subtitle),
                        icon = Icons.Default.ChatBubble,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onChatClick
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    SettingsCategoryRow(
                        title = stringResource(R.string.settings_sounds_group),
                        subtitle = stringResource(R.string.settings_sounds_summary),
                        icon = Icons.Default.Notifications,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onSoundsClick
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    SettingsCategoryRow(
                        title = stringResource(R.string.settings_network_group),
                        subtitle = stringResource(R.string.settings_proxy_type),
                        icon = Icons.Default.Wifi,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onConnectionClick
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    SettingsCategoryRow(
                        title = stringResource(R.string.backup_title),
                        subtitle = stringResource(R.string.backup_settings_subtitle),
                        icon = Icons.Default.Backup,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onBackupClick
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCategoryRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconContainerColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            })
            .padding(horizontal = RowPaddingHorizontal, vertical = RowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(IconContainerSize)
                .background(iconContainerColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(IconSize)
            )
        }
        Spacer(modifier = Modifier.width(RowPaddingHorizontal))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(ArrowSize)
        )
    }
}
