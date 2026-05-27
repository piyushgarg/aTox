// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.avatar.NotificationAvatarLoader
import ltd.evilcorp.atox.ui.notification.MessageNotificationFactory
import ltd.evilcorp.atox.ui.notification.CallNotificationFactory
import ltd.evilcorp.atox.ui.notification.CHANNEL_MESSAGE
import ltd.evilcorp.atox.ui.notification.CHANNEL_FRIEND_REQUEST
import ltd.evilcorp.atox.ui.notification.CHANNEL_CALL
import ltd.evilcorp.atox.ui.notification.CHANNEL_GROUP_MESSAGE
import ltd.evilcorp.atox.infrastructure.util.PermissionManager
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.FriendRequest
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.domain.feature.INotificationHelper

private const val TAG = "NotificationHelper"

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager,
    private val avatarLoader: NotificationAvatarLoader,
    private val messageNotificationFactory: MessageNotificationFactory,
    private val callNotificationFactory: CallNotificationFactory
) : INotificationHelper {
    private val notifier = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val messageChannel = NotificationChannelCompat.Builder(CHANNEL_MESSAGE, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.messages))
            .setDescription(context.getString(R.string.messages_incoming))
            .setSound(null, null)
            .build()

        val friendChannel = NotificationChannelCompat.Builder(CHANNEL_FRIEND_REQUEST, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.friend_requests))
            .build()

        val callChannel = NotificationChannelCompat.Builder(CHANNEL_CALL, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.calls))
            .setVibrationEnabled(true)
            .setSound(null, null)
            .build()

        val groupChannel = NotificationChannelCompat.Builder(CHANNEL_GROUP_MESSAGE, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.groups))
            .setDescription(context.getString(R.string.messages_incoming))
            .setSound(null, null)
            .build()

        notifier.createNotificationChannelsCompat(listOf(messageChannel, friendChannel, callChannel, groupChannel))
    }

    override fun dismissNotifications(publicKey: PublicKey) = notifier.cancel(publicKey.string().hashCode())

    override fun dismissCallNotification(publicKey: PublicKey) = notifier.cancel(publicKey.string().hashCode() + CHANNEL_CALL.hashCode())

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMessageNotification(
        contact: Contact,
        message: String,
        outgoing: Boolean = false,
        silent: Boolean = outgoing,
    ) {
        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Received message, notifications disallowed")
            return
        }
        val builder = messageNotificationFactory.buildMessageNotification(contact, message, outgoing, silent)
        notifier.notify(contact.publicKey.hashCode(), builder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showFriendRequestNotification(friendRequest: FriendRequest, silent: Boolean) {
        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Received friend request, notifications disallowed")
            return
        }
        val builder = messageNotificationFactory.buildFriendRequestNotification(friendRequest, silent)
        notifier.notify(friendRequest.publicKey.hashCode(), builder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showGroupMessageNotification(
        groupName: String,
        senderName: String,
        message: String,
        silent: Boolean = false,
    ) {
        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Received group message, notifications disallowed")
            return
        }
        val builder = messageNotificationFactory.buildGroupMessageNotification(groupName, senderName, message, silent)
        notifier.notify("$groupName$senderName".hashCode(), builder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showGroupInviteNotification(
        friendPk: String,
        groupName: String,
        silent: Boolean = false
    ) {
        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Received group invite, notifications disallowed")
            return
        }
        val builder = messageNotificationFactory.buildGroupInviteNotification(friendPk, groupName, silent)
        notifier.notify(friendPk.hashCode(), builder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showOngoingCallNotification(contact: Contact) {
        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Call ongoing, notifications disallowed")
            return
        }

        dismissCallNotification(PublicKey(contact.publicKey))

        val builder = callNotificationFactory.buildOngoingCallNotification(contact)

        try {
            notifier.notify(contact.publicKey.hashCode() + CHANNEL_CALL.hashCode(), builder.build())
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to post CallStyle notification, falling back to standard style", e)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                builder.setStyle(null)
            }
            try {
                notifier.notify(contact.publicKey.hashCode() + CHANNEL_CALL.hashCode(), builder.build())
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to post fallback call notification", ex)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showPendingCallNotification(status: UserStatus, c: Contact) {
        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Call pending, notifications disallowed")
            return
        }

        val builder = callNotificationFactory.buildPendingCallNotification(status, c)
        val notification = builder.build().apply {
            flags = flags.or(androidx.core.app.NotificationCompat.FLAG_INSISTENT)
        }

        notifier.notify(c.publicKey.hashCode() + CHANNEL_CALL.hashCode(), notification)
    }

    fun invalidateAvatar(uri: android.net.Uri) {
        avatarLoader.invalidateAvatar(uri)
    }
}
