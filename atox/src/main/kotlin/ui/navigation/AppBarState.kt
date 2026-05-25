package ltd.evilcorp.atox.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow

data class AppBarConfig(
    val title: @Composable () -> Unit = {},
    val navigationIcon: @Composable (() -> Unit)? = null,
    val actions: @Composable (RowScope.() -> Unit)? = null,
    val containerColor: Color? = null
)

object AppBarStateHolder {
    val config = MutableStateFlow<AppBarConfig?>(null)
}
