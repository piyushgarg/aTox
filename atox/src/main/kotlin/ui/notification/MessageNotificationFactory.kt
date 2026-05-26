// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.MainActivity
import ltd.evilcorp.atox.CONTACT_PUBLIC_KEY
import ltd.evilcorp.atox.FOCUS_ON_MESSAGE_BOX
import ltd.evilcorp.atox.receiver.Action
import ltd.evilcorp.atox.receiver.ActionReceiver
import ltd.evilcorp.atox.receiver.KEY_ACTION
import ltd.evilcorp.atox.receiver.KEY_CONTACT_PK
import ltd.evilcorp.atox.receiver.KEY_TEXT_REPLY
import ltd.evilcorp.atox.ui.avatar.NotificationAvatarLoader
import ltd.evilcorp.atox.util.PendingIntentCompat
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.model.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

internal const val CHANNEL_MESSAGE = "aTox messages"
internal const val CHANNEL_FRIEND_REQUEST = "aTox friend requests"
internal const val CHANNEL_GROUP_MESSAGE = "aTox group messages"

@Singleton
class MessageNotificationFactory @Inject constructor(
    private val context: Context,
    private val avatarLoader: NotificationAvatarLoader
) {
    private val notifierOld = context.getSystemService<NotificationManager>()!!

    fun buildMessageNotification(
        contact: Contact,
        message: String,
        outgoing: Boolean,
        silent: Boolean
    ): NotificationCompat.Builder {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_MESSAGE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(contact.name.ifEmpty { context.getText(R.string.contact_default_name) })
            .setContentText(message)
            .setContentIntent(deepLinkToChat(PublicKey(contact.publicKey)))
            .setAutoCancel(true)
            .setSilent(silent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val avatarBitmap = avatarLoader.loadAvatar(contact.avatarUri)
            val icon = avatarBitmap?.let { IconCompat.createWithBitmap(it) }

            val chatPartner = Person.Builder()
                .setName(contact.name.ifEmpty { context.getText(R.string.contact_default_name) })
                .setKey(if (outgoing) "myself" else contact.publicKey)
                .setIcon(icon)
                .setImportant(true)
                .build()

            val style = notifierOld.activeNotifications.find { it.notification.group == contact.publicKey }?.notification?.let {
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it)
            } ?: NotificationCompat.MessagingStyle(chatPartner)

            style.messages.add(
                NotificationCompat.MessagingStyle.Message(message, System.currentTimeMillis(), chatPartner)
            )

            notificationBuilder
                .setStyle(style)
                .setGroup(contact.publicKey)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.addAction(
                NotificationCompat.Action.Builder(
                    IconCompat.createWithResource(context, R.drawable.ic_send),
                    context.getString(R.string.reply),
                    PendingIntentCompat.getBroadcast(
                        context,
                        contact.publicKey.hashCode(),
                        Intent(context, ActionReceiver::class.java).putExtra(KEY_CONTACT_PK, contact.publicKey),
                        PendingIntent.FLAG_UPDATE_CURRENT,
                        mutable = true
                    )
                )
                .addRemoteInput(
                    RemoteInput.Builder(KEY_TEXT_REPLY)
                        .setLabel(context.getString(R.string.message))
                        .build()
                )
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .setAllowGeneratedReplies(true)
                .build()
            )
        } else {
            notificationBuilder.addAction(
                NotificationCompat.Action.Builder(
                    IconCompat.createWithResource(context, R.drawable.ic_send),
                    context.getString(R.string.reply),
                    deepLinkToChat(PublicKey(contact.publicKey), focusMessageBox = true)
                )
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .build()
            )
        }

        notificationBuilder.addAction(
            NotificationCompat.Action.Builder(
                null,
                context.getString(R.string.mark_as_read),
                PendingIntentCompat.getBroadcast(
                    context,
                    "${contact.publicKey}_mark_as_read".hashCode(),
                    Intent(context, ActionReceiver::class.java)
                        .putExtra(KEY_CONTACT_PK, contact.publicKey)
                        .putExtra(KEY_ACTION, Action.MarkAsRead),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            ).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ).build()
        )

        return notificationBuilder
    }

    fun buildFriendRequestNotification(friendRequest: FriendRequest, silent: Boolean): NotificationCompat.Builder {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            friendRequest.publicKey.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_FRIEND_REQUEST)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle(context.getString(R.string.friend_request_from, friendRequest.publicKey))
            .setContentText(friendRequest.message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(silent)
    }

    fun buildGroupMessageNotification(
        groupName: String,
        senderName: String,
        message: String,
        silent: Boolean
    ): NotificationCompat.Builder {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            "$groupName$senderName".hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_GROUP_MESSAGE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle("$senderName @ $groupName")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(silent)
    }

    fun buildGroupInviteNotification(
        friendPk: String,
        groupName: String,
        silent: Boolean
    ): NotificationCompat.Builder {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            friendPk.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_GROUP_MESSAGE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(context.getString(R.string.group_invite))
            .setContentText(context.getString(R.string.group_invite_confirm, groupName))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(silent)
    }

    private fun deepLinkToChat(publicKey: PublicKey, focusMessageBox: Boolean = false): PendingIntent {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CONTACT_PUBLIC_KEY, publicKey.string())
            putExtra(FOCUS_ON_MESSAGE_BOX, focusMessageBox)
        }
        return PendingIntent.getActivity(
            context,
            publicKey.string().hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
