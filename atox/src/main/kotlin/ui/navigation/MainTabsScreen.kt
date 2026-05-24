package ltd.evilcorp.atox.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import ltd.evilcorp.atox.ui.theme.AToxMotion

val LocalTabPadding = compositionLocalOf { PaddingValues(0.dp) }

@Composable
fun MainTabsScreen(
    currentRoute: String,
    attentionCount: Int,
    hapticEnabled: Boolean,
    onTabSelected: (String) -> Unit,
    topBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val attentionLabel = when {
        attentionCount <= 0 -> ""
        attentionCount > 99 -> "99+"
        else -> attentionCount.toString()
    }
    val selectTab: (String) -> Unit = { route ->
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onTabSelected(route)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = topBar,
        bottomBar = {} // Empty bottom bar so Scaffold never resizes content!
    ) { paddingValues ->
        val density = androidx.compose.ui.platform.LocalDensity.current
        val navigationBarsInsets = WindowInsets.navigationBars
        val bottomPadding = if (currentRoute.startsWith("chat/")) {
            0.dp
        } else {
            // M3 NavigationBar default height is 80.dp.
            // We also add the navigation bars inset padding at the bottom.
            80.dp + with(density) { navigationBarsInsets.getBottom(density).toDp() }
        }

        val tabPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = bottomPadding
        )
        CompositionLocalProvider(LocalTabPadding provides tabPadding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                // The NavHost content occupies full screen height (under top bar)
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    content()
                }

                // Place the NavigationBar here inside the Box to prevent Scaffold content resizing!
                AnimatedVisibility(
                    visible = !currentRoute.startsWith("chat/"),
                    enter = fadeIn(animationSpec = tween(300)) + 
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(300, easing = AToxMotion.EmphasizedDecelerate)
                            ),
                    exit = fadeOut(animationSpec = tween(200)) + 
                           slideOutVertically(
                               targetOffsetY = { it },
                               animationSpec = tween(200, easing = AToxMotion.EmphasizedAccelerate)
                           ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 8.dp,
                            windowInsets = WindowInsets(0) // Prevent double insets padding
                        ) {
                            NavigationBarItem(
                                selected = currentRoute == AppRoutes.Chats,
                                onClick = { selectTab(AppRoutes.Chats) },
                                icon = {
                                    if (attentionCount > 0) {
                                        BadgedBox(badge = { Badge { Text(attentionLabel) } }) {
                                            Icon(Icons.Default.Email, contentDescription = "Chats")
                                        }
                                    } else {
                                        Icon(Icons.Default.Email, contentDescription = "Chats")
                                    }
                                },
                                label = { Text(stringResource(R.string.chats)) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == AppRoutes.AddContactTab,
                                onClick = { selectTab(AppRoutes.AddContactTab) },
                                icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact") },
                                label = { Text(stringResource(R.string.add_contact_tab)) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == AppRoutes.Profile,
                                onClick = { selectTab(AppRoutes.Profile) },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text(stringResource(R.string.profile)) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == AppRoutes.Settings,
                                onClick = { selectTab(AppRoutes.Settings) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text(stringResource(R.string.settings)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
