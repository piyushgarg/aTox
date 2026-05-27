// SPDX-FileCopyrightText: 2026 aTox contributors
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import ltd.evilcorp.atox.R

private const val BYTES_PER_KB = 1024.0

internal fun formatSize(context: Context, bytes: Long): String {
    if (bytes <= 0) return "0 ${context.getString(R.string.size_bytes)}"
    val units = arrayOf(
        context.getString(R.string.size_bytes),
        context.getString(R.string.size_kb),
        context.getString(R.string.size_mb),
        context.getString(R.string.size_gb)
    )
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(BYTES_PER_KB)).toInt()
    return String.format(
        java.util.Locale.getDefault(),
        "%.2f %s",
        bytes / Math.pow(BYTES_PER_KB, digitGroups.toDouble()),
        units[digitGroups]
    )
}

internal fun launchRingtonePicker(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    title: String,
    type: Int,
    currentUri: String,
) {
    launcher.launch(
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                currentUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
                    ?: RingtoneManager.getDefaultUri(type)
            )
        }
    )
}
