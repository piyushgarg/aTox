package ltd.evilcorp.atox.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.core.model.BootstrapNodeSource
import ltd.evilcorp.core.model.FtAutoAccept
import ltd.evilcorp.atox.receiver.BootReceiver
import ltd.evilcorp.core.tox.save.ProxyType
import ltd.evilcorp.core.model.UserSettings
import ltd.evilcorp.core.repository.UserSettingsRepository

class Settings @Inject constructor(
    private val ctx: Context,
    private val repository: UserSettingsRepository,
) {
    val state: StateFlow<UserSettings> = repository.settings

    init {
        val persisted = repository.settings.value.runAtStartup
        val actual = isBootReceiverEnabled()
        if (persisted != actual) {
            repository.updateRunAtStartup(actual)
        }
    }

    var udpEnabled: Boolean
        get() = repository.settings.value.udpEnabled
        set(enabled) = repository.updateUdpEnabled(enabled)

    var runAtStartup: Boolean
        get() = repository.settings.value.runAtStartup
        set(runAtStartup) {
            repository.updateRunAtStartup(runAtStartup)
            val state = if (runAtStartup) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            ctx.packageManager.setComponentEnabledSetting(
                ComponentName(ctx, BootReceiver::class.java),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }

    var autoAwayEnabled: Boolean
        get() = repository.settings.value.autoAwayEnabled
        set(enabled) = repository.updateAutoAwayEnabled(enabled)

    var autoAwaySeconds: Long
        get() = repository.settings.value.autoAwaySeconds
        set(seconds) = repository.updateAutoAwaySeconds(seconds)

    var proxyType: ProxyType
        get() = repository.settings.value.proxyType
        set(type) = repository.updateProxyType(type)

    var proxyAddress: String
        get() = repository.settings.value.proxyAddress
        set(address) = repository.updateProxyAddress(address)

    var proxyPort: Int
        get() = repository.settings.value.proxyPort
        set(port) = repository.updateProxyPort(port)

    var ftAutoAccept: FtAutoAccept
        get() = repository.settings.value.ftAutoAccept
        set(autoAccept) = repository.updateFtAutoAccept(autoAccept)

    var bootstrapNodeSource: BootstrapNodeSource
        get() = repository.settings.value.bootstrapNodeSource
        set(source) = repository.updateBootstrapNodeSource(source)

    var disableScreenshots: Boolean
        get() = repository.settings.value.disableScreenshots
        set(disable) = repository.updateDisableScreenshots(disable)

    var confirmQuitting: Boolean
        get() = repository.settings.value.confirmQuitting
        set(confirm) = repository.updateConfirmQuitting(confirm)

    var confirmCalling: Boolean
        get() = repository.settings.value.confirmCalling
        set(confirm) = repository.updateConfirmCalling(confirm)

    var hapticEnabled: Boolean
        get() = repository.settings.value.hapticEnabled
        set(enabled) = repository.updateHapticEnabled(enabled)

    var autoSaveToDownloads: Boolean
        get() = repository.settings.value.autoSaveToDownloads
        set(enabled) = repository.updateAutoSaveToDownloads(enabled)

    private fun isBootReceiverEnabled(): Boolean =
        ctx.packageManager.getComponentEnabledSetting(
            ComponentName(ctx, BootReceiver::class.java),
        ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
}
