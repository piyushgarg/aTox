package ltd.evilcorp.atox.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow

data class AppBarConfig @OptIn(ExperimentalMaterial3Api::class) constructor(
    val title: @Composable () -> Unit = {},
    val navigationIcon: @Composable (() -> Unit)? = null,
    val actions: @Composable (RowScope.() -> Unit)? = null,
    val containerColor: Color? = null,
    val isLarge: Boolean = false,
    val scrollBehavior: TopAppBarScrollBehavior? = null
)

object AppBarStateHolder {
    val config = MutableStateFlow<AppBarConfig?>(null)
    private val configs = mutableMapOf<String, AppBarConfig>()
    private var currentRoute: String? = null

    private fun getBaseRoute(route: String?): String? {
        if (route == null) return null
        return route.substringBefore('/').substringBefore('?')
    }

    fun register(route: String, cfg: AppBarConfig) {
        val baseRoute = getBaseRoute(route) ?: return
        configs[baseRoute] = cfg
        if (baseRoute == getBaseRoute(currentRoute)) {
            config.value = cfg
        }
    }

    fun unregister(route: String) {
        val baseRoute = getBaseRoute(route) ?: return
        configs.remove(baseRoute)
        if (baseRoute == getBaseRoute(currentRoute)) {
            config.value = null
        }
    }

    fun updateRoute(route: String?) {
        currentRoute = route
        val baseRoute = getBaseRoute(route)
        config.value = configs[baseRoute]
    }
}
