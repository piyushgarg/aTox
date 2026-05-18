package ltd.evilcorp.atox.appearance

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ltd.evilcorp.core.model.UserSettings
import ltd.evilcorp.core.repository.UserSettingsRepository

@Singleton
class AppearanceManager @Inject constructor(
    private val context: Context,
    private val repository: UserSettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val appearance: StateFlow<AppAppearance> = repository.settings
        .map { it.toAppAppearance() }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = repository.settings.value.toAppAppearance(),
        )

    fun updateThemeMode(themeMode: Int) {
        if (appearance.value.themeMode == themeMode) return
        repository.updateThemeMode(themeMode)
    }

    fun updateDynamicColorEnabled(enabled: Boolean) {
        if (appearance.value.dynamicColorEnabled == enabled) return
        repository.updateDynamicColorEnabled(enabled)
    }

    fun updateAccentColorSeed(accentColorSeed: Int) {
        if (appearance.value.accentColorSeed == accentColorSeed) return
        repository.updateAccentColorSeed(accentColorSeed)
    }

    fun updateLocaleTag(localeTag: String) {
        if (appearance.value.localeTag == localeTag) return
        repository.updateLocaleTag(localeTag)
        applyLocaleTag(context, localeTag)
    }

    companion object {
        fun applyPersistedAppearance(context: Context) {
            val appearance = readPersistedAppearance(context)
            applyLocaleTag(context, appearance.localeTag)
        }

        fun readPersistedAppearance(context: Context): AppAppearance {
            return UserSettingsRepository.readBlocking(context).toAppAppearance()
        }

        private fun applyLocaleTag(context: Context, localeTag: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                if (localeManager != null) {
                    localeManager.applicationLocales = localeListForPlatform(localeTag)
                    return
                }
            }

            AppCompatDelegate.setApplicationLocales(localeListForCompat(localeTag))
        }

        private fun localeListForPlatform(localeTag: String): LocaleList =
            if (localeTag.isBlank()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(localeTag)
            }

        private fun localeListForCompat(localeTag: String): LocaleListCompat =
            if (localeTag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(localeTag)
            }
    }
}

private fun UserSettings.toAppAppearance(): AppAppearance =
    AppAppearance(
        themeMode = themeMode,
        dynamicColorEnabled = dynamicColorEnabled,
        accentColorSeed = accentColorSeed,
        localeTag = localeTag,
    )
