package ltd.evilcorp.atox.receiver

import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.util.PermissionManager
import ltd.evilcorp.atox.App
import ltd.evilcorp.atox.MainActivity
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.PendingIntent
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.core.tox.save.ToxSaveStatus

private const val ENCRYPTED = "aTox profile encrypted"
private const val TAG = "BootReceiver"

class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var toxStarter: ToxStarter

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var settings: Settings

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            (context.applicationContext as App).component.inject(this)
            if (!settings.runAtStartup) {
                Log.w(TAG, "Boot completed broadcast received but startup is disabled in settings. Skipping.")
                return
            }
            if (toxStarter.tryLoadTox(null) == ToxSaveStatus.Encrypted) {
                Log.i(TAG, "Telling the user to unlock their profile")
                if (!permissionManager.canPostNotifications()) {
                    Log.w(TAG, "Missing notify-permission")
                    return
                }

                val channel = NotificationChannelCompat.Builder(ENCRYPTED, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.atox_profile_locked))
                    .setDescription(context.getString(R.string.channel_profile_locked_explanation))
                    .build()

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    ENCRYPTED.hashCode(),
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, ENCRYPTED)
                    .setContentTitle(context.getString(R.string.atox_profile_locked))
                    .setContentText(context.getString(R.string.tap_to_unlock_and_start_atox))
                    .setContentIntent(pendingIntent)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setAutoCancel(true)
                    .build()
                val notifier = NotificationManagerCompat.from(context)
                notifier.createNotificationChannel(channel)
                notifier.notify(ENCRYPTED.hashCode(), notification)
            }
        }
    }
}
