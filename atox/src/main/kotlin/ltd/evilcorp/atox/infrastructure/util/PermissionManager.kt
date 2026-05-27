package ltd.evilcorp.atox.infrastructure.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(private val context: Context) {

    /**
     * Checks if a specific permission is granted.
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns the permission string for recording audio.
     */
    val recordAudioPermission: String = Manifest.permission.RECORD_AUDIO

    /**
     * Returns the permission string for posting notifications.
     */
    val notificationPermission: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        ""
    }

    /**
     * Returns the permission string for using full screen intents.
     */
    val fullScreenIntentPermission: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Manifest.permission.USE_FULL_SCREEN_INTENT
    } else {
        ""
    }

    /**
     * Checks if record audio permission is granted.
     */
    fun canRecordAudio(): Boolean = hasPermission(recordAudioPermission)

    /**
     * Checks if notification permission is granted.
     */
    fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(notificationPermission)
        } else {
            true
        }
    }

    /**
     * Checks if full screen intent permission is granted.
     */
    fun canUseFullScreenIntent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hasPermission(fullScreenIntentPermission)
        } else {
            true
        }
    }
}
