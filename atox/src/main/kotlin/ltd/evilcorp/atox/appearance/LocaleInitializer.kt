package ltd.evilcorp.atox.appearance

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleInitializer @Inject constructor(
    private val context: Context
) {
    fun updateLocale(localeTag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (localeManager != null) {
                localeManager.applicationLocales = if (localeTag.isBlank()) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(localeTag)
                }
            }
        } else {
            AppCompatDelegate.setApplicationLocales(
                if (localeTag.isBlank()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(localeTag)
                }
            )
        }
    }
}
