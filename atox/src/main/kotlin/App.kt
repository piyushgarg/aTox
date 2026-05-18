package ltd.evilcorp.atox

import android.app.Application
import androidx.annotation.VisibleForTesting
import ltd.evilcorp.atox.appearance.AppearanceManager
import ltd.evilcorp.atox.di.AppComponent
import ltd.evilcorp.atox.di.DaggerAppComponent

class App : Application() {
    val component: AppComponent by lazy {
        componentOverride ?: DaggerAppComponent.factory().create(applicationContext)
    }

    @VisibleForTesting
    var componentOverride: AppComponent? = null

    override fun onCreate() {
        super.onCreate()
        AppearanceManager.applyPersistedAppearance(this)
    }
}
