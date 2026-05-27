// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import ltd.evilcorp.atox.ui.navigation.AppBarConfig
import ltd.evilcorp.atox.ui.navigation.AppBarStateHolder

@Suppress("FunctionNaming")
@Composable
fun AtoxAppBar(
    route: String,
    config: AppBarConfig
) {
    SideEffect {
        AppBarStateHolder.register(route, config)
    }

    DisposableEffect(route) {
        onDispose {
            AppBarStateHolder.unregister(route)
        }
    }
}
