package ltd.evilcorp.core.repository

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.dataStoreFile
import ltd.evilcorp.core.profile.ProfileManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.BackupDestination
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.DEFAULT_ACCENT_COLOR_SEED
import ltd.evilcorp.domain.features.settings.model.DEFAULT_THEME_MODE
import ltd.evilcorp.domain.features.settings.model.FtAutoAccept
import ltd.evilcorp.domain.features.settings.model.AppSound
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository

private const val MAX_VOLUME = 100

private val dataStoreInstances = java.util.concurrent.ConcurrentHashMap<String, androidx.datastore.core.DataStore<Preferences>>()

private fun getUserSettingsDataStore(context: Context): androidx.datastore.core.DataStore<Preferences> {
    val activeProfileId = ProfileManager.getActiveProfileId(context)
    return dataStoreInstances.getOrPut(activeProfileId) {
        val storeName = if (activeProfileId == ProfileManager.DEFAULT_PROFILE_ID) {
            "user_settings.preferences_pb"
        } else {
            "user_settings_$activeProfileId.preferences_pb"
        }
        PreferenceDataStoreFactory.create(
            migrations = listOf(
                SharedPreferencesMigration(
                    context = context,
                    sharedPreferencesName = "${context.packageName}_preferences",
                )
            ),
            produceFile = { context.dataStoreFile(storeName) }
        )
    }
}

private val Context.dataStore get() = getUserSettingsDataStore(this)

@Singleton
class UserSettingsRepositoryImpl @Inject constructor(
    private val context: Context,
    private val scope: CoroutineScope,
) : IUserSettingsRepository {

    private val activeProfileIdFlow = callbackFlow {
        val prefs = context.getSharedPreferences("atox_multi_profiles", Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "active_profile_id") {
                val profileId = ProfileManager.getActiveProfileId(context)
                trySend(profileId)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        // Emit initial value
        trySend(ProfileManager.getActiveProfileId(context))
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val settings: StateFlow<UserSettings> = activeProfileIdFlow
        .flatMapLatest { profileId ->
            val storeName = if (profileId == ProfileManager.DEFAULT_PROFILE_ID) {
                "user_settings.preferences_pb"
            } else {
                "user_settings_$profileId.preferences_pb"
            }
            val datastore = dataStoreInstances.getOrPut(profileId) {
                PreferenceDataStoreFactory.create(
                    migrations = listOf(
                        SharedPreferencesMigration(
                            context = context,
                            sharedPreferencesName = "${context.packageName}_preferences",
                        )
                    ),
                    produceFile = { context.dataStoreFile(storeName) }
                )
            }
            datastore.data.map(::toUserSettings)
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = UserSettings(),
        )

    override suspend fun updateThemeMode(themeMode: Int) = update(Keys.themeMode, themeMode)

    override suspend fun updateDynamicColorEnabled(enabled: Boolean) = update(Keys.dynamicColorEnabled, enabled)

    override suspend fun updateAccentColorSeed(accentColorSeed: Int) = update(Keys.accentColorSeed, accentColorSeed)

    override suspend fun updateLocaleTag(localeTag: String) = update(Keys.localeTag, localeTag)

    override suspend fun updateDateFormatPreference(preference: DateFormatPreference) =
        update(Keys.dateFormatPreferenceOrdinal, preference.ordinal)

    override suspend fun updateTimeFormatPreference(preference: TimeFormatPreference) =
        update(Keys.timeFormatPreferenceOrdinal, preference.ordinal)

    override suspend fun updateUdpEnabled(enabled: Boolean) = update(Keys.udpEnabled, enabled)

    override suspend fun updateRunAtStartup(enabled: Boolean) = update(Keys.runAtStartup, enabled)

    override suspend fun updateAutoAwayEnabled(enabled: Boolean) = update(Keys.autoAwayEnabled, enabled)

    override suspend fun updateAutoAwaySeconds(seconds: Long) = update(Keys.autoAwaySeconds, seconds)

    override suspend fun updateProxyType(type: ProxyType) = update(Keys.proxyTypeOrdinal, type.ordinal)

    override suspend fun updateProxyAddress(address: String) = update(Keys.proxyAddress, address)

    override suspend fun updateProxyPort(port: Int) = update(Keys.proxyPort, port)

    override suspend fun updateFtAutoAccept(value: FtAutoAccept) = update(Keys.ftAutoAcceptOrdinal, value.ordinal)

    override suspend fun updateBootstrapNodeSource(value: BootstrapNodeSource) = update(Keys.bootstrapNodeSourceOrdinal, value.ordinal)

    override suspend fun updateDisableScreenshots(disable: Boolean) = update(Keys.disableScreenshots, disable)

    override suspend fun updateConfirmQuitting(confirm: Boolean) = update(Keys.confirmQuitting, confirm)

    override suspend fun updateConfirmCalling(confirm: Boolean) = update(Keys.confirmCalling, confirm)
    override suspend fun updateEnableReplies(enabled: Boolean) = update(Keys.enableReplies, enabled)

    override suspend fun updateSentMessageSoundVolume(volume: Int) = update(Keys.sentMessageSoundVolume, volume.coerceIn(0, MAX_VOLUME))
    override suspend fun updateSentMessageSoundUri(uri: String) = update(Keys.sentMessageSoundUri, uri)

    override suspend fun updateCallSound(sound: AppSound) = update(Keys.callSoundOrdinal, sound.ordinal)

    override suspend fun updateCallSoundVolume(volume: Int) = update(Keys.callSoundVolume, volume.coerceIn(0, MAX_VOLUME))

    override suspend fun updateCallRingtoneUri(uri: String) = update(Keys.callRingtoneUri, uri)

    override suspend fun updateNotificationSoundVolume(volume: Int) = update(Keys.notificationSoundVolume, volume.coerceIn(0, MAX_VOLUME))
    override suspend fun updateNotificationSoundUri(uri: String) = update(Keys.notificationSoundUri, uri)

    override suspend fun updateActiveChatSoundVolume(volume: Int) = update(Keys.activeChatSoundVolume, volume.coerceIn(0, MAX_VOLUME))
    override suspend fun updateActiveChatSoundUri(uri: String) = update(Keys.activeChatSoundUri, uri)

    override suspend fun updateHapticEnabled(enabled: Boolean) = update(Keys.hapticEnabled, enabled)

    override suspend fun updateAutoSaveToDownloads(enabled: Boolean) = update(Keys.autoSaveToDownloads, enabled)

    override suspend fun updateAutoSaveDirectoryUri(uri: String) = update(Keys.autoSaveDirectoryUri, uri)


    override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) = update(Keys.automaticBackupEnabled, enabled)

    override suspend fun updateBackupFrequency(frequency: BackupFrequency) = update(Keys.backupFrequencyOrdinal, frequency.ordinal)

    override suspend fun updateBackupGoogleAccount(account: String) = update(Keys.backupGoogleAccount, account)

    override suspend fun updateBackupUseCellular(enabled: Boolean) = update(Keys.backupUseCellular, enabled)

    override suspend fun updateBackupDestinationOrdinals(ordinals: Set<Int>) {
        update(Keys.backupDestinationOrdinals, ordinals.map(Int::toString).toSet())
    }

    override suspend fun updateLocalBackupDirectoryUri(uri: String) = update(Keys.localBackupDirectoryUri, uri)

    override suspend fun updateLastLocalBackupTimeMs(timeMs: Long) = update(Keys.lastLocalBackupTimeMs, timeMs)

    override suspend fun updateLastLocalBackupSizeKb(sizeKb: Long) = update(Keys.lastLocalBackupSizeKb, sizeKb)

    override suspend fun updateLastGoogleBackupTimeMs(timeMs: Long) = update(Keys.lastGoogleBackupTimeMs, timeMs)

    override suspend fun updateLastGoogleBackupSizeKb(sizeKb: Long) = update(Keys.lastGoogleBackupSizeKb, sizeKb)

    private suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    companion object {
        fun readBlocking(context: Context): UserSettings = runBlocking {
            toUserSettings(context.dataStore.data.first())
        }

        @Suppress("CyclomaticComplexMethod")
        private fun toUserSettings(preferences: Preferences): UserSettings =
            UserSettings(
                themeMode = preferences[Keys.themeMode] ?: DEFAULT_THEME_MODE,
                dynamicColorEnabled = preferences[Keys.dynamicColorEnabled] ?: true,
                accentColorSeed = preferences[Keys.accentColorSeed] ?: DEFAULT_ACCENT_COLOR_SEED,
                localeTag = preferences[Keys.localeTag] ?: "",
                dateFormatPreference = DateFormatPreference.entries[
                    preferences[Keys.dateFormatPreferenceOrdinal] ?: DateFormatPreference.System.ordinal
                ],
                timeFormatPreference = TimeFormatPreference.entries[
                    preferences[Keys.timeFormatPreferenceOrdinal] ?: TimeFormatPreference.System.ordinal
                ],
                udpEnabled = preferences[Keys.udpEnabled] ?: true,
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
                sentMessageSoundVolume = preferences[Keys.sentMessageSoundVolume] ?: 24,
                sentMessageSoundUri = preferences[Keys.sentMessageSoundUri] ?: "",
                callSound = AppSound.entries[
                    preferences[Keys.callSoundOrdinal] ?: AppSound.Pulse.ordinal
                ],
                callSoundVolume = preferences[Keys.callSoundVolume] ?: 72,
                callRingtoneUri = preferences[Keys.callRingtoneUri] ?: "",
                notificationSoundVolume = preferences[Keys.notificationSoundVolume] ?: 52,
                notificationSoundUri = preferences[Keys.notificationSoundUri] ?: "",
                activeChatSoundVolume = preferences[Keys.activeChatSoundVolume] ?: 28,
                activeChatSoundUri = preferences[Keys.activeChatSoundUri] ?: "",
                hapticEnabled = preferences[Keys.hapticEnabled] ?: true,
                autoSaveToDownloads = preferences[Keys.autoSaveToDownloads] ?: true,
                autoSaveDirectoryUri = preferences[Keys.autoSaveDirectoryUri] ?: "",
                automaticBackupEnabled = preferences[Keys.automaticBackupEnabled] ?: false,
                backupFrequency = BackupFrequency.entries[
                    preferences[Keys.backupFrequencyOrdinal] ?: BackupFrequency.Off.ordinal
                ],
                backupGoogleAccount = preferences[Keys.backupGoogleAccount] ?: "",
                backupUseCellular = preferences[Keys.backupUseCellular] ?: false,
                backupDestinationOrdinals = preferences[Keys.backupDestinationOrdinals]
                    ?.mapNotNull(String::toIntOrNull)
                    ?.filter { it in BackupDestination.entries.indices }
                    ?.toSet()
                    ?.takeIf { it.isNotEmpty() }
                    ?: setOf(BackupDestination.Local.ordinal),
                localBackupDirectoryUri = preferences[Keys.localBackupDirectoryUri] ?: "",
                lastLocalBackupTimeMs = preferences[Keys.lastLocalBackupTimeMs] ?: 0L,
                lastLocalBackupSizeKb = preferences[Keys.lastLocalBackupSizeKb] ?: 0L,
                lastGoogleBackupTimeMs = preferences[Keys.lastGoogleBackupTimeMs] ?: 0L,
                lastGoogleBackupSizeKb = preferences[Keys.lastGoogleBackupSizeKb] ?: 0L,
                enableReplies = preferences[Keys.enableReplies] ?: true,
            )
    }

    private object Keys {
        val themeMode = intPreferencesKey("theme")
        val dynamicColorEnabled = booleanPreferencesKey("dynamic_color_enabled")
        val accentColorSeed = intPreferencesKey("accent_color_seed")
        val localeTag = stringPreferencesKey("locale_tag")
        val dateFormatPreferenceOrdinal = intPreferencesKey("date_format_preference")
        val timeFormatPreferenceOrdinal = intPreferencesKey("time_format_preference")
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
        val sentMessageSoundVolume = intPreferencesKey("sent_message_sound_volume")
        val sentMessageSoundUri = stringPreferencesKey("sent_message_sound_uri")
        val callSoundOrdinal = intPreferencesKey("call_sound")
        val callSoundVolume = intPreferencesKey("call_sound_volume")
        val callRingtoneUri = stringPreferencesKey("call_ringtone_uri")
        val notificationSoundVolume = intPreferencesKey("notification_sound_volume")
        val notificationSoundUri = stringPreferencesKey("notification_sound_uri")
        val activeChatSoundVolume = intPreferencesKey("active_chat_sound_volume")
        val activeChatSoundUri = stringPreferencesKey("active_chat_sound_uri")
        val hapticEnabled = booleanPreferencesKey("haptic_enabled")
        val autoSaveToDownloads = booleanPreferencesKey("auto_save_to_downloads")
        val autoSaveDirectoryUri = stringPreferencesKey("auto_save_directory_uri")
        val automaticBackupEnabled = booleanPreferencesKey("automatic_backup_enabled")
        val backupFrequencyOrdinal = intPreferencesKey("backup_frequency")
        val backupGoogleAccount = stringPreferencesKey("backup_google_account")
        val backupUseCellular = booleanPreferencesKey("backup_use_cellular")
        val backupDestinationOrdinals = stringSetPreferencesKey("backup_destination_ordinals")
        val localBackupDirectoryUri = stringPreferencesKey("local_backup_directory_uri")
        val lastLocalBackupTimeMs = longPreferencesKey("last_local_backup_time_ms")
        val lastLocalBackupSizeKb = longPreferencesKey("last_local_backup_size_kb")
        val lastGoogleBackupTimeMs = longPreferencesKey("last_google_backup_time_ms")
        val lastGoogleBackupSizeKb = longPreferencesKey("last_google_backup_size_kb")
        val enableReplies = booleanPreferencesKey("enable_replies")
    }
}
