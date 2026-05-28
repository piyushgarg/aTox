package ltd.evilcorp.atox.ui.common

import android.content.Context
import android.text.format.DateFormat
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatChatTime(
    context: Context,
    timestamp: Long,
    timeFormatPreference: TimeFormatPreference,
): String {
    val pattern = when (timeFormatPreference) {
        TimeFormatPreference.System -> {
            val is24 = try {
                DateFormat.is24HourFormat(context)
            } catch (e: Throwable) {
                true
            }
            if (is24) "HH:mm" else "h:mm a"
        }
        TimeFormatPreference.Hours24 -> "HH:mm"
        TimeFormatPreference.Hours12 -> "h:mm a"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}

fun formatPresenceText(
    context: Context,
    contact: Contact,
    dateFormatPreference: DateFormatPreference,
    timeFormatPreference: TimeFormatPreference,
): PresenceText {
    if (contact.typing) {
        return PresenceText(
            text = context.getString(R.string.contact_typing),
            color = PresenceTone.Accent,
        )
    }

    if (contact.connectionStatus != ConnectionStatus.None) {
        val label = when (contact.status) {
            UserStatus.None -> R.string.chat_status_online
            UserStatus.Away -> R.string.chat_status_away
            UserStatus.Busy -> R.string.chat_status_busy
        }
        val tone = when (contact.status) {
            UserStatus.None -> PresenceTone.Online
            UserStatus.Away -> PresenceTone.Away
            UserStatus.Busy -> PresenceTone.Busy
        }
        return PresenceText(context.getString(label), tone)
    }

    if (contact.lastOnline > 0L) {
        val lastSeen = formatLastSeen(context, contact.lastOnline, dateFormatPreference, timeFormatPreference)
        return PresenceText(
            text = context.getString(R.string.contact_last_seen, lastSeen),
            color = PresenceTone.Muted,
        )
    }

    return PresenceText(
        text = context.getString(R.string.chat_status_offline),
        color = PresenceTone.Muted,
    )
}

private fun formatLastSeen(
    context: Context,
    timestamp: Long,
    dateFormatPreference: DateFormatPreference,
    timeFormatPreference: TimeFormatPreference,
): String {
    if (isSameDay(timestamp, System.currentTimeMillis())) {
        return context.getString(R.string.contact_last_seen_today, formatChatTime(context, timestamp, timeFormatPreference))
    }

    val datePattern = when (dateFormatPreference) {
        DateFormatPreference.System -> null
        DateFormatPreference.DMY -> "dd/MM/yyyy"
        DateFormatPreference.DMYDots -> "dd.MM.yyyy"
        DateFormatPreference.MDY -> "MM/dd/yyyy"
        DateFormatPreference.YMD -> "yyyy/MM/dd"
    }
    val datePart = if (datePattern == null) {
        java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(Date(timestamp))
    } else {
        SimpleDateFormat(datePattern, Locale.getDefault()).format(Date(timestamp))
    }
    return datePart
}

fun formatMessageDateHeader(
    context: Context,
    timestamp: Long,
    dateFormatPreference: DateFormatPreference,
): String {
    val now = System.currentTimeMillis()
    val diffDays = TimeUnit.MILLISECONDS.toDays(now) - TimeUnit.MILLISECONDS.toDays(timestamp)
    return when (diffDays) {
        0L -> context.getString(R.string.chat_date_today)
        1L -> context.getString(R.string.chat_date_yesterday)
        else -> {
            val datePattern = when (dateFormatPreference) {
                DateFormatPreference.System -> null
                DateFormatPreference.DMY -> "dd/MM/yyyy"
                DateFormatPreference.DMYDots -> "dd.MM.yyyy"
                DateFormatPreference.MDY -> "MM/dd/yyyy"
                DateFormatPreference.YMD -> "yyyy/MM/dd"
            }
            if (datePattern == null) {
                java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(Date(timestamp))
            } else {
                SimpleDateFormat(datePattern, Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}

private fun isSameDay(first: Long, second: Long): Boolean {
    val firstCalendar = Calendar.getInstance().apply { timeInMillis = first }
    val secondCalendar = Calendar.getInstance().apply { timeInMillis = second }
    return firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR) &&
        firstCalendar.get(Calendar.DAY_OF_YEAR) == secondCalendar.get(Calendar.DAY_OF_YEAR)
}

data class PresenceText(
    val text: String,
    val color: PresenceTone,
)

enum class PresenceTone {
    Online,
    Away,
    Busy,
    Accent,
    Muted,
}
