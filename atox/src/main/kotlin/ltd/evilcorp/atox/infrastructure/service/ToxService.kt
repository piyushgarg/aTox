// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2021-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.service

import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.App
import ltd.evilcorp.atox.MainActivity
import ltd.evilcorp.atox.infrastructure.util.PendingIntentCompat
import ltd.evilcorp.atox.infrastructure.util.PermissionManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.tox.ITox
import ltd.evilcorp.core.tox.save.ToxSaveStatus

private const val TAG = "ToxService"
private const val NOTIFICATION_ID = 1984

@AndroidEntryPoint
class ToxService : LifecycleService() {
    private val channelId = "ToxService"
    private var connectionStatus: ConnectionStatus? = null
    private val notifier by lazy { NotificationManagerCompat.from(this) }

    @Inject
    lateinit var tox: ITox

    @Inject
    lateinit var toxStarter: ToxStarter

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var lifecycleController: ToxServiceLifecycleController

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName("Tox Service")
            .build()

        notifier.createNotificationChannel(channel)
    }

    private fun subTextFor(status: ConnectionStatus) = when (status) {
        ConnectionStatus.None -> getText(R.string.atox_offline)
        ConnectionStatus.TCP -> getText(R.string.atox_connected_with_tcp)
        ConnectionStatus.UDP -> getText(R.string.atox_connected_with_udp)
    }

    private fun notificationFor(status: ConnectionStatus?): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntentCompat.getActivity(this, 0, notificationIntent, 0)
            }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setContentTitle(getString(R.string.tox_service_running))

        if (status != null) {
            builder.setContentText(subTextFor(status))
        }

        return builder.build()
    }

    override fun onCreate() {
        super.onCreate()

        if (!tox.started) {
            if (toxStarter.tryLoadTox(null) != ToxSaveStatus.Ok) {
                Log.e(TAG, "Tox service started without a Tox save")
                stopSelf()
            }
        }

        if (!permissionManager.canPostNotifications()) {
            Log.w(TAG, "Notifications disallowed")
        }

        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notificationFor(connectionStatus),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notificationFor(connectionStatus))
        }

        lifecycleController.start(this) { newStatus ->
            connectionStatus = newStatus
            if (permissionManager.canPostNotifications()) {
                notifier.notify(NOTIFICATION_ID, notificationFor(connectionStatus))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleController.stop()
        tox.stop()
    }
}
