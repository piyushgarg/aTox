package ltd.evilcorp.atox.infrastructure.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.evilcorp.atox.infrastructure.receiver.BootReceiver
import ltd.evilcorp.domain.features.settings.IRunAtStartupController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of IRunAtStartupController.
 * Toggles the BootReceiver component state using Android Context.
 */
@Singleton
class AndroidRunAtStartupController @Inject constructor(
    @ApplicationContext private val context: Context,
) : IRunAtStartupController {

    override fun setRunAtStartup(enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, BootReceiver::class.java),
            state,
            PackageManager.DONT_KILL_APP,
        )
    }
}
