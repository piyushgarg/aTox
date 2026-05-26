// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.MainActivity
import ltd.evilcorp.atox.CONTACT_PUBLIC_KEY
import ltd.evilcorp.atox.receiver.Action
import ltd.evilcorp.atox.receiver.ActionReceiver
import ltd.evilcorp.atox.receiver.KEY_ACTION
import ltd.evilcorp.atox.receiver.KEY_CONTACT_PK
import ltd.evilcorp.atox.util.PendingIntentCompat
import ltd.evilcorp.atox.util.PermissionManager
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FINGERPRINT_LEN
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.UserStatus
import javax.inject.Inject
import javax.inject.Singleton

internal const val CHANNEL_CALL = "aTox calls"
private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

@Singleton
class CallNotificationFactory @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) {
    fun buildOngoingCallNotification(contact: Contact): NotificationCompat.Builder {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CONTACT_PUBLIC_KEY, contact.publicKey)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            contact.publicKey.hashCode() + CHANNEL_CALL.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hangupIntent = PendingIntentCompat.getBroadcast(
            context,
            "${contact.publicKey}_end_call".hashCode(),
            Intent(context, ActionReceiver::class.java)
                .putExtra(KEY_CONTACT_PK, contact.publicKey)
                .putExtra(KEY_ACTION, Action.CallEnd),
            PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val callPerson = Person.Builder()
            .setName(contact.name.ifEmpty { context.getString(R.string.contact_default_name) })
            .setKey(contact.publicKey)
            .setImportant(true)
            .build()

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_CALL)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(context.getString(R.string.ongoing_call))
            .setContentText(
                context.getString(
                    R.string.in_call_with,
                    contact.name.ifEmpty { context.getString(R.string.contact_default_name) },
                ),
            )
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addExtras(Bundle().apply { putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true) })
            .addAction(
                NotificationCompat.Action.Builder(
                    IconCompat.createWithResource(context, R.drawable.ic_call_end),
                    context.getString(R.string.end_call),
                    hangupIntent
                ).build()
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.setStyle(NotificationCompat.CallStyle.forOngoingCall(callPerson, hangupIntent))
        }

        return notificationBuilder
    }

    fun buildPendingCallNotification(status: UserStatus, c: Contact): NotificationCompat.Builder {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_CALL)

        val pendingIntent = deepLinkToChat(PublicKey(c.publicKey))
        if (permissionManager.canUseFullScreenIntent()) {
            notificationBuilder
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(pendingIntent, true)
        } else {
            notificationBuilder.setContentIntent(pendingIntent)
        }

        notificationBuilder
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(context.getString(R.string.incoming_call))
            .setContentText(context.getString(R.string.incoming_call_from, c.name.ifEmpty { c.publicKey.take(FINGERPRINT_LEN) }))
            .addAction(
                NotificationCompat.Action.Builder(
                    IconCompat.createWithResource(context, R.drawable.ic_call),
                    context.getString(R.string.accept),
                    PendingIntentCompat.getBroadcast(
                        context,
                        "${c.publicKey}_accept_call".hashCode(),
                        Intent(context, ActionReceiver::class.java)
                            .putExtra(KEY_CONTACT_PK, c.publicKey)
                            .putExtra(KEY_ACTION, Action.CallAccept),
                        PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                )
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_CALL)
                .build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    IconCompat.createWithResource(context, R.drawable.ic_not_interested),
                    context.getString(R.string.reject),
                    PendingIntentCompat.getBroadcast(
                        context,
                        "${c.publicKey}_reject_call".hashCode(),
                        Intent(context, ActionReceiver::class.java)
                            .putExtra(KEY_CONTACT_PK, c.publicKey)
                            .putExtra(KEY_ACTION, Action.CallReject),
                        PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                )
                .build()
            )
            .setDeleteIntent(
                PendingIntentCompat.getBroadcast(
                    context,
                    "${c.publicKey}_ignore_call".hashCode(),
                    Intent(context, ActionReceiver::class.java)
                        .putExtra(KEY_CONTACT_PK, c.publicKey)
                        .putExtra(KEY_ACTION, Action.CallIgnore),
                    PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .setSound(RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE))
            .setSilent(status == UserStatus.Busy)

        return notificationBuilder
    }

    private fun deepLinkToChat(publicKey: PublicKey, focusMessageBox: Boolean = false): PendingIntent {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CONTACT_PUBLIC_KEY, publicKey.string())
            putExtra(ltd.evilcorp.atox.FOCUS_ON_MESSAGE_BOX, focusMessageBox)
        }
        return PendingIntent.getActivity(
            context,
            publicKey.string().hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
