package ltd.evilcorp.atox.ui

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.getSystemService
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.atox.receiver.Action
import ltd.evilcorp.atox.receiver.ActionReceiver
import ltd.evilcorp.atox.receiver.KEY_ACTION
import ltd.evilcorp.atox.receiver.KEY_CONTACT_PK
import ltd.evilcorp.atox.receiver.KEY_TEXT_REPLY
import ltd.evilcorp.atox.util.PendingIntentCompat
import ltd.evilcorp.atox.util.PermissionManager
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.MainActivity
import ltd.evilcorp.atox.CONTACT_PUBLIC_KEY
import ltd.evilcorp.atox.FOCUS_ON_MESSAGE_BOX
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.model.FINGERPRINT_LEN

private const val TAG = "NotificationHelper"

private const val MESSAGE = "aTox messages"
private const val FRIEND_REQUEST = "aTox friend requests"
private const val CALL = "aTox calls"
private const val GROUP_MESSAGE = "aTox group messages"

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) {
    private val notifier = NotificationManagerCompat.from(context)
    private val notifierOld = context.getSystemService<NotificationManager>()!!

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val messageChannel = NotificationChannelCompat.Builder(MESSAGE, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.messages))
            .setDescription(context.getString(R.string.messages_incoming))
            .setSound(null, null)
            .build()

        val friendChannel = NotificationChannelCompat.Builder(FRIEND_REQUEST, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.friend_requests))
            .build()

        val callChannel = NotificationChannelCompat.Builder(CALL, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.calls))
            .setVibrationEnabled(true)
            .setSound(null, null)
            .build()

        val groupChannel = NotificationChannelCompat.Builder(GROUP_MESSAGE, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.groups))
            .setDescription(context.getString(R.string.messages_incoming))
            .setSound(null, null)
            .build()

        notifier.createNotificationChannelsCompat(listOf(messageChannel, friendChannel, callChannel, groupChannel))
    }

    fun dismissNotifications(publicKey: PublicKey) = notifier.cancel(publicKey.string().hashCode())

    private val circleTransform = object : Transformation {
        override fun transform(bitmap: Bitmap): Bitmap {
            val output = createBitmap(bitmap.width, bitmap.height)
            val canvas = Canvas(output)
            val paint = Paint()
            val rect = Rect(0, 0, bitmap.width, bitmap.height)

            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawCircle(bitmap.width / 2.0f, bitmap.height / 2.0f, bitmap.width / 2.0f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            if (bitmap != output) {
                bitmap.recycle()
            }
            return output
        }

        override fun key() = "circleTransform"
    }

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

        val notificationBuilder = NotificationCompat.Builder(context, MESSAGE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(contact.name.ifEmpty { context.getText(R.string.contact_default_name) })
            .setContentText(message)
            .setContentIntent(deepLinkToChat(PublicKey(contact.publicKey)))
            .setAutoCancel(true)
            .setSilent(silent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val icon = if (contact.avatarUri.isNotEmpty()) {
                IconCompat.createWithBitmap(Picasso.get().load(contact.avatarUri).transform(circleTransform).get())
            } else {
                null
            }

            val chatPartner = Person.Builder()
                .setName(contact.name.ifEmpty { context.getText(R.string.contact_default_name) })
                .setKey(if (outgoing) "myself" else contact.publicKey)
                .setIcon(icon)
                .setImportant(true)
                .build()

            val style =
                notifierOld.activeNotifications.find { it.notification.group == contact.publicKey }?.notification?.let {
                    NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it)
                } ?: NotificationCompat.MessagingStyle(chatPartner)

            style.messages.add(
                NotificationCompat.MessagingStyle.Message(message, System.currentTimeMillis(), chatPartner),
            )

            notificationBuilder
                .setStyle(style)
                .setGroup(contact.publicKey)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.addAction(
                NotificationCompat.Action
                    .Builder(
                        IconCompat.createWithResource(context, R.drawable.ic_send),
                        context.getString(R.string.reply),
                        PendingIntentCompat.getBroadcast(
                            context,
                            contact.publicKey.hashCode(),
                            Intent(context, ActionReceiver::class.java).putExtra(KEY_CONTACT_PK, contact.publicKey),
                            PendingIntent.FLAG_UPDATE_CURRENT,
                            mutable = true,
                        ),
                    )
                    .addRemoteInput(
                        RemoteInput.Builder(KEY_TEXT_REPLY)
                            .setLabel(context.getString(R.string.message))
                            .build(),
                    )
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                    .setAllowGeneratedReplies(true)
                    .build(),
            )
        } else {
            notificationBuilder.addAction(
                NotificationCompat.Action
                    .Builder(
                        IconCompat.createWithResource(context, R.drawable.ic_send),
                        context.getString(R.string.reply),
                        deepLinkToChat(PublicKey(contact.publicKey), focusMessageBox = true),
                    )
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                    .build(),
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
                    PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            ).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ).build(),
        )

        notifier.notify(contact.publicKey.hashCode(), notificationBuilder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showFriendRequestNotification(friendRequest: FriendRequest, silent: Boolean) {
        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Received friend request, notifications disallowed")
            return
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            friendRequest.publicKey.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, FRIEND_REQUEST)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle(context.getString(R.string.friend_request_from, friendRequest.publicKey))
            .setContentText(friendRequest.message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(silent)

        notifier.notify(friendRequest.publicKey.hashCode(), notificationBuilder.build())
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

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            "$groupName$senderName".hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, GROUP_MESSAGE)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle("$senderName @ $groupName")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(silent)

        notifier.notify("$groupName$senderName".hashCode(), notificationBuilder.build())
    }

    fun dismissCallNotification(pk: PublicKey) = notifier.cancel(pk.string().hashCode() + CALL.hashCode())

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showOngoingCallNotification(contact: Contact) {
        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Call ongoing, notifications disallowed")
            return
        }

        dismissCallNotification(PublicKey(contact.publicKey))

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CONTACT_PUBLIC_KEY, contact.publicKey)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            contact.publicKey.hashCode() + CALL.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CALL)
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
            .addAction(
                NotificationCompat.Action
                    .Builder(
                        IconCompat.createWithResource(context, R.drawable.ic_call_end),
                        context.getString(R.string.end_call),
                        PendingIntentCompat.getBroadcast(
                            context,
                            "${contact.publicKey}_end_call".hashCode(),
                            Intent(context, ActionReceiver::class.java)
                                .putExtra(KEY_CONTACT_PK, contact.publicKey)
                                .putExtra(KEY_ACTION, Action.CallEnd),
                            PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    )
                    .build(),
            )
            .setOngoing(true)
            .setSilent(true)

        notifier.notify(contact.publicKey.hashCode() + CALL.hashCode(), notificationBuilder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showPendingCallNotification(status: UserStatus, c: Contact) {
        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Call pending, notifications disallowed")
            return
        }

        val notificationBuilder = NotificationCompat.Builder(context, CALL)

        val pendingIntent = deepLinkToChat(PublicKey(c.publicKey))
        if (permissionManager.canUseFullScreenIntent()) {
            notificationBuilder
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(pendingIntent, true)
        } else {
            notificationBuilder.setContentIntent(pendingIntent)
        }

        val notification = notificationBuilder
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(context.getString(R.string.incoming_call))
            .setContentText(context.getString(R.string.incoming_call_from, c.name.ifEmpty { c.publicKey.take(FINGERPRINT_LEN) }))
            .addAction(
                NotificationCompat.Action
                    .Builder(
                        IconCompat.createWithResource(context, R.drawable.ic_call),
                        context.getString(R.string.accept),
                        PendingIntentCompat.getBroadcast(
                            context,
                            "${c.publicKey}_accept_call".hashCode(),
                            Intent(context, ActionReceiver::class.java)
                                .putExtra(KEY_CONTACT_PK, c.publicKey)
                                .putExtra(KEY_ACTION, Action.CallAccept),
                            PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    )
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_CALL)
                    .build(),
            )
            .addAction(
                NotificationCompat.Action
                    .Builder(
                        IconCompat.createWithResource(context, R.drawable.ic_not_interested),
                        context.getString(R.string.reject),
                        PendingIntentCompat.getBroadcast(
                            context,
                            "${c.publicKey}_reject_call".hashCode(),
                            Intent(context, ActionReceiver::class.java)
                                .putExtra(KEY_CONTACT_PK, c.publicKey)
                                .putExtra(KEY_ACTION, Action.CallReject),
                            PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    )
                    .build(),
            )
            .setDeleteIntent(
                PendingIntentCompat.getBroadcast(
                    context,
                    "${c.publicKey}_ignore_call".hashCode(),
                    Intent(context, ActionReceiver::class.java)
                        .putExtra(KEY_CONTACT_PK, c.publicKey)
                        .putExtra(KEY_ACTION, Action.CallIgnore),
                    PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .setSound(RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE))
            .setSilent(status == UserStatus.Busy)
            .build()
            .apply {
                flags = flags.or(NotificationCompat.FLAG_INSISTENT)
            }

        notifier.notify(c.publicKey.hashCode() + CALL.hashCode(), notification)
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
