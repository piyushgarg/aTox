package ltd.evilcorp.atox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ltd.evilcorp.atox.appearance.AppearanceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppearanceManager.applyPersistedAppearance(this)

        // Background cleanup of shared cache files on startup to prevent leaks
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val sharedCacheDir = File(cacheDir, "shared")
                if (sharedCacheDir.exists() && sharedCacheDir.isDirectory) {
                    sharedCacheDir.listFiles()?.forEach { file ->
                        file.delete()
                    }
                }
            }
        }
    }
}
