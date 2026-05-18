package ltd.evilcorp.core.model

import ltd.evilcorp.core.tox.save.ProxyType

const val DEFAULT_THEME_MODE = -1
const val DEFAULT_ACCENT_COLOR_SEED = 0xFF3F51B5.toInt()

data class UserSettings(
    val themeMode: Int = DEFAULT_THEME_MODE,
    val dynamicColorEnabled: Boolean = true,
    val accentColorSeed: Int = DEFAULT_ACCENT_COLOR_SEED,
    val localeTag: String = "",
    val udpEnabled: Boolean = false,
    val runAtStartup: Boolean = false,
    val autoAwayEnabled: Boolean = false,
    val autoAwaySeconds: Long = 180L,
    val proxyType: ProxyType = ProxyType.None,
    val proxyAddress: String = "",
    val proxyPort: Int = 0,
    val ftAutoAccept: FtAutoAccept = FtAutoAccept.None,
    val bootstrapNodeSource: BootstrapNodeSource = BootstrapNodeSource.BuiltIn,
    val disableScreenshots: Boolean = false,
    val confirmQuitting: Boolean = true,
    val confirmCalling: Boolean = true,
    val hapticEnabled: Boolean = true,
    val autoSaveToDownloads: Boolean = true,
)
