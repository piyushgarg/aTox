package ltd.evilcorp.atox.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import ltd.evilcorp.atox.ui.theme.AToxMotion

val LocalTabPadding = compositionLocalOf { PaddingValues(0.dp) }

@Suppress("FunctionNaming")
@Composable
fun MainTabsScreen(
    currentRoute: String,
    attentionCount: Int,
    hapticEnabled: Boolean,
    onTabSelected: (String) -> Unit,
    onAddContactClick: () -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onJoinGroupClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val isSubScreen = currentRoute.contains("Chat") || 
                      currentRoute.contains("CreateGroup") ||
                      currentRoute.contains("JoinGroup") ||
                      currentRoute.contains("Search")

    val density = androidx.compose.ui.platform.LocalDensity.current
    val navigationBarsInsets = WindowInsets.navigationBars

    val targetBottomPadding = if (isSubScreen) {
        0.dp
    } else {
        80.dp + with(density) { navigationBarsInsets.getBottom(density).toDp() }
    }

    val animatedBottomPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetBottomPadding,
        animationSpec = tween(
            durationMillis = 300,
            easing = AToxMotion.EmphasizedDecelerate
        ),
        label = "bottomPaddingAnimation"
    )

    val tabPadding = PaddingValues(
        bottom = animatedBottomPadding
    )

    CompositionLocalProvider(LocalTabPadding provides tabPadding) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                content()
            }

            val showBottomBar = !isSubScreen

            AToxFAB(
                currentRoute = currentRoute,
                visible = showBottomBar && currentRoute.endsWith("Chats"),
                hapticEnabled = hapticEnabled,
                onAddContactClick = onAddContactClick,
                onCreateGroupClick = onCreateGroupClick,
                onJoinGroupClick = onJoinGroupClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = animatedBottomPadding + 16.dp, end = 16.dp)
            )

            AToxBottomBar(
                currentRoute = currentRoute,
                visible = showBottomBar,
                attentionCount = attentionCount,
                hapticEnabled = hapticEnabled,
                onTabSelected = onTabSelected,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private const val MAX_ATTENTION_DISPLAY_COUNT = 99

@Suppress("FunctionNaming")
@Composable
fun AToxBottomBar(
    currentRoute: String?,
    visible: Boolean,
    attentionCount: Int,
    hapticEnabled: Boolean,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val attentionLabel = remember(attentionCount) {
        when {
            attentionCount <= 0 -> ""
            attentionCount > MAX_ATTENTION_DISPLAY_COUNT -> "$MAX_ATTENTION_DISPLAY_COUNT+"
            else -> attentionCount.toString()
        }
    }
    val selectTab: (String) -> Unit = { route ->
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onTabSelected(route)
    }

    AnimatedVisibility(
        visible = visible,
        enter = AToxMotion.bottomBarEnter(),
        exit = AToxMotion.bottomBarExit(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
            ) {
                val chatsRouteName = AppRoutes.Chats::class.qualifiedName!!
                val profileRouteName = AppRoutes.Profile::class.qualifiedName!!
                val settingsRouteName = AppRoutes.Settings::class.qualifiedName!!

                NavigationBarItem(
                    selected = currentRoute?.endsWith("Chats") == true,
                    onClick = { selectTab(chatsRouteName) },
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
                    selected = currentRoute?.endsWith("Profile") == true,
                    onClick = { selectTab(profileRouteName) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text(stringResource(R.string.profile)) }
                )
                NavigationBarItem(
                    selected = currentRoute?.endsWith("Settings") == true,
                    onClick = { selectTab(settingsRouteName) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(stringResource(R.string.settings)) }
                )
            }
        }
    }
}

@Suppress("FunctionNaming", "UnusedParameter")
@Composable
fun AToxFAB(
    currentRoute: String?,
    visible: Boolean,
    hapticEnabled: Boolean,
    onAddContactClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = AToxMotion.fabEnter(),
        exit = AToxMotion.fabExit(),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            val elevationZero = 0.dp
            val roundedCorner16 = 16.dp
            FloatingActionButton(
                onClick = {
                    if (hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    isFabMenuExpanded = !isFabMenuExpanded
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(roundedCorner16),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = elevationZero,
                    pressedElevation = elevationZero,
                    hoveredElevation = elevationZero,
                    focusedElevation = elevationZero
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.navigation_add)
                )
            }

            DropdownMenu(
                expanded = isFabMenuExpanded,
                onDismissRequest = { isFabMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_contact)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        isFabMenuExpanded = false
                        onAddContactClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.create_group)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        isFabMenuExpanded = false
                        onCreateGroupClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.join_group)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        isFabMenuExpanded = false
                        onJoinGroupClick()
                    }
                )
            }
        }
    }
}
