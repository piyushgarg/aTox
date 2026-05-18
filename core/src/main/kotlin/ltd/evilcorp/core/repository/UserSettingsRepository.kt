package ltd.evilcorp.core.repository

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ltd.evilcorp.core.model.BootstrapNodeSource
import ltd.evilcorp.core.model.DEFAULT_ACCENT_COLOR_SEED
import ltd.evilcorp.core.model.DEFAULT_THEME_MODE
import ltd.evilcorp.core.model.FtAutoAccept
import ltd.evilcorp.core.model.UserSettings
import ltd.evilcorp.core.tox.save.ProxyType

private val Context.userSettingsDataStore by preferencesDataStore(
    name = "user_settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "${context.packageName}_preferences",
            ),
        )
    },
)

@Singleton
class UserSettingsRepository @Inject constructor(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings: StateFlow<UserSettings> = context.userSettingsDataStore.data
        .map(::toUserSettings)
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = readBlocking(context),
        )

    fun updateThemeMode(themeMode: Int) = update(Keys.themeMode, themeMode)

    fun updateDynamicColorEnabled(enabled: Boolean) = update(Keys.dynamicColorEnabled, enabled)

    fun updateAccentColorSeed(accentColorSeed: Int) = update(Keys.accentColorSeed, accentColorSeed)

    fun updateLocaleTag(localeTag: String) = update(Keys.localeTag, localeTag)

    fun updateUdpEnabled(enabled: Boolean) = update(Keys.udpEnabled, enabled)

    fun updateRunAtStartup(enabled: Boolean) = update(Keys.runAtStartup, enabled)

    fun updateAutoAwayEnabled(enabled: Boolean) = update(Keys.autoAwayEnabled, enabled)

    fun updateAutoAwaySeconds(seconds: Long) = update(Keys.autoAwaySeconds, seconds)

    fun updateProxyType(type: ProxyType) = update(Keys.proxyTypeOrdinal, type.ordinal)

    fun updateProxyAddress(address: String) = update(Keys.proxyAddress, address)

    fun updateProxyPort(port: Int) = update(Keys.proxyPort, port)

    fun updateFtAutoAccept(value: FtAutoAccept) = update(Keys.ftAutoAcceptOrdinal, value.ordinal)

    fun updateBootstrapNodeSource(value: BootstrapNodeSource) = update(Keys.bootstrapNodeSourceOrdinal, value.ordinal)

    fun updateDisableScreenshots(disable: Boolean) = update(Keys.disableScreenshots, disable)

    fun updateConfirmQuitting(confirm: Boolean) = update(Keys.confirmQuitting, confirm)

    fun updateConfirmCalling(confirm: Boolean) = update(Keys.confirmCalling, confirm)

    fun updateHapticEnabled(enabled: Boolean) = update(Keys.hapticEnabled, enabled)

    fun updateAutoSaveToDownloads(enabled: Boolean) = update(Keys.autoSaveToDownloads, enabled)

    private fun <T> update(key: Preferences.Key<T>, value: T) {
        scope.launch {
            context.userSettingsDataStore.edit { preferences ->
                preferences[key] = value
            }
        }
    }

    companion object {
        fun readBlocking(context: Context): UserSettings = runBlocking {
            toUserSettings(context.userSettingsDataStore.data.first())
        }

        private fun toUserSettings(preferences: Preferences): UserSettings =
            UserSettings(
                themeMode = preferences[Keys.themeMode] ?: DEFAULT_THEME_MODE,
                dynamicColorEnabled = preferences[Keys.dynamicColorEnabled] ?: true,
                accentColorSeed = preferences[Keys.accentColorSeed] ?: DEFAULT_ACCENT_COLOR_SEED,
                localeTag = preferences[Keys.localeTag] ?: "",
                udpEnabled = preferences[Keys.udpEnabled] ?: false,
                runAtStartup = preferences[Keys.runAtStartup] ?: false,
                autoAwayEnabled = preferences[Keys.autoAwayEnabled] ?: false,
                autoAwaySeconds = preferences[Keys.autoAwaySeconds] ?: 180L,
                proxyType = ProxyType.entries[preferences[Keys.proxyTypeOrdinal] ?: ProxyType.None.ordinal],
                proxyAddress = preferences[Keys.proxyAddress] ?: "",
                proxyPort = preferences[Keys.proxyPort] ?: 0,
                ftAutoAccept = FtAutoAccept.entries[preferences[Keys.ftAutoAcceptOrdinal] ?: FtAutoAccept.None.ordinal],
                bootstrapNodeSource = BootstrapNodeSource.entries[
                    preferences[Keys.bootstrapNodeSourceOrdinal] ?: BootstrapNodeSource.BuiltIn.ordinal
                ],
                disableScreenshots = preferences[Keys.disableScreenshots] ?: false,
                confirmQuitting = preferences[Keys.confirmQuitting] ?: true,
                confirmCalling = preferences[Keys.confirmCalling] ?: true,
                hapticEnabled = preferences[Keys.hapticEnabled] ?: true,
                autoSaveToDownloads = preferences[Keys.autoSaveToDownloads] ?: true,
            )
    }

    private object Keys {
        val themeMode = intPreferencesKey("theme")
        val dynamicColorEnabled = booleanPreferencesKey("dynamic_color_enabled")
        val accentColorSeed = intPreferencesKey("accent_color_seed")
        val localeTag = stringPreferencesKey("locale_tag")
        val udpEnabled = booleanPreferencesKey("udp_enabled")
        val runAtStartup = booleanPreferencesKey("run_at_startup")
        val autoAwayEnabled = booleanPreferencesKey("auto_away_enabled")
        val autoAwaySeconds = longPreferencesKey("auto_away_seconds")
        val proxyTypeOrdinal = intPreferencesKey("proxy_type")
        val proxyAddress = stringPreferencesKey("proxy_address")
        val proxyPort = intPreferencesKey("proxy_port")
        val ftAutoAcceptOrdinal = intPreferencesKey("ft_auto_accept")
        val bootstrapNodeSourceOrdinal = intPreferencesKey("bootstrap_node_source")
        val disableScreenshots = booleanPreferencesKey("disable_screenshots")
        val confirmQuitting = booleanPreferencesKey("confirm_quitting")
        val confirmCalling = booleanPreferencesKey("confirm_calling")
        val hapticEnabled = booleanPreferencesKey("haptic_enabled")
        val autoSaveToDownloads = booleanPreferencesKey("auto_save_to_downloads")
    }
}
