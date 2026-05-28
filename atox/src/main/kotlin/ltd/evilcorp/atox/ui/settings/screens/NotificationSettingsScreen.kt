// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.screens

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.settings.common.SettingsGroup
import ltd.evilcorp.atox.ui.settings.common.SettingsClickableRow
import ltd.evilcorp.atox.ui.settings.common.SettingsSliderRow

private val ContentPaddingTop = 16.dp
private val ContentPaddingBottomDefault = 32.dp
private val HorizontalMargin = 16.dp
private val SpacingSpacedBy = 16.dp

@Suppress("FunctionNaming")
@Composable
fun NotificationSettingsScreen(
    paddingValues: PaddingValues,
    sentMessageSoundVolume: Int,
    callSoundVolume: Int,
    notificationSoundVolume: Int,
    activeChatSoundVolume: Int,
    sentMessageSoundUri: String,
    callRingtoneUri: String,
    notificationSoundUri: String,
    activeChatSoundUri: String,
    onVolumeChanged: (SoundPickerTarget, Int) -> Unit,
    onSoundPickerClick: (SoundPickerTarget, String, Int) -> Unit,
    performHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            SettingsGroup(title = stringResource(R.string.settings_sound_group_sending)) {
                SettingsClickableRow(
                    title = stringResource(R.string.settings_sent_sound_title),
                    subtitle = soundTitle(context, sentMessageSoundUri, RingtoneManager.TYPE_NOTIFICATION),
                    marqueeSubtitle = true
                ) {
                    performHaptic()
                    onSoundPickerClick(
                        SoundPickerTarget.Sent,
                        sentMessageSoundUri,
                        RingtoneManager.TYPE_NOTIFICATION
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                SettingsSliderRow(
                    title = stringResource(R.string.settings_sent_sound_volume_title),
                    subtitle = stringResource(R.string.settings_sent_sound_volume_subtitle, sentMessageSoundVolume),
                    value = sentMessageSoundVolume.toFloat(),
                    valueRange = 0f..100f,
                    steps = 19,
                    onValueChangeFinished = performHaptic,
                ) { onVolumeChanged(SoundPickerTarget.Sent, it.toInt()) }
            }
        }
        item {
            SettingsGroup(title = stringResource(R.string.settings_sound_group_calls)) {
                SettingsClickableRow(
                    title = stringResource(R.string.settings_call_sound_title),
                    subtitle = soundTitle(context, callRingtoneUri, RingtoneManager.TYPE_RINGTONE),
                    marqueeSubtitle = true
                ) {
                    performHaptic()
                    onSoundPickerClick(
                        SoundPickerTarget.Call,
                        callRingtoneUri,
                        RingtoneManager.TYPE_RINGTONE
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                SettingsSliderRow(
                    title = stringResource(R.string.settings_call_sound_volume_title),
                    subtitle = stringResource(R.string.settings_call_sound_volume_subtitle, callSoundVolume),
                    value = callSoundVolume.toFloat(),
                    valueRange = 0f..100f,
                    steps = 19,
                    onValueChangeFinished = performHaptic,
                ) { onVolumeChanged(SoundPickerTarget.Call, it.toInt()) }
            }
        }
        item {
            SettingsGroup(title = stringResource(R.string.settings_sound_group_notifications)) {
                SettingsClickableRow(
                    title = stringResource(R.string.settings_notification_sound_title),
                    subtitle = soundTitle(context, notificationSoundUri, RingtoneManager.TYPE_NOTIFICATION),
                    marqueeSubtitle = true
                ) {
                    performHaptic()
                    onSoundPickerClick(
                        SoundPickerTarget.Notification,
                        notificationSoundUri,
                        RingtoneManager.TYPE_NOTIFICATION
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                SettingsSliderRow(
                    title = stringResource(R.string.settings_notification_sound_volume_title),
                    subtitle = stringResource(R.string.settings_notification_sound_volume_subtitle, notificationSoundVolume),
                    value = notificationSoundVolume.toFloat(),
                    valueRange = 0f..100f,
                    steps = 19,
                    onValueChangeFinished = performHaptic,
                ) { onVolumeChanged(SoundPickerTarget.Notification, it.toInt()) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                SettingsClickableRow(
                    title = stringResource(R.string.settings_active_chat_sound_title),
                    subtitle = soundTitle(context, activeChatSoundUri, RingtoneManager.TYPE_NOTIFICATION),
                    marqueeSubtitle = true
                ) {
                    performHaptic()
                    onSoundPickerClick(
                        SoundPickerTarget.ActiveChat,
                        activeChatSoundUri,
                        RingtoneManager.TYPE_NOTIFICATION
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                SettingsSliderRow(
                    title = stringResource(R.string.settings_active_chat_sound_volume_title),
                    subtitle = stringResource(R.string.settings_active_chat_sound_volume_subtitle, activeChatSoundVolume),
                    value = activeChatSoundVolume.toFloat(),
                    valueRange = 0f..100f,
                    steps = 19,
                    onValueChangeFinished = performHaptic,
                ) { onVolumeChanged(SoundPickerTarget.ActiveChat, it.toInt()) }
            }
        }
    }
}

private fun soundTitle(context: Context, uriString: String, type: Int): String {
    return try {
        val uri = uriString.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(type)
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
            ?: context.getString(R.string.settings_call_sound_default)
    } catch (e: Exception) {
        context.getString(R.string.settings_call_sound_default)
    }
}

enum class SoundPickerTarget {
    Sent,
    Call,
    Notification,
    ActiveChat,
}
