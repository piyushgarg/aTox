package ltd.evilcorp.atox.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import ltd.evilcorp.atox.SharedContent
import ltd.evilcorp.domain.features.call.CallState

/**
 * Handles Tox ID link processing and navigates to the add contact destination.
 */
@Composable
fun ToxLinkNavigationHandler(
    navController: NavController,
    toxLinkManager: ToxLinkManager,
) {
    val pendingToxId by toxLinkManager.pendingToxId.collectAsStateWithLifecycle()
    LaunchedEffect(pendingToxId) {
        pendingToxId?.let { toxId ->
            navController.navigate(AppRoutes.AddContact(toxId))
            toxLinkManager.clear()
        }
    }
}

/**
 * Monitors the call state and triggers automatic screen routing (e.g. entering
 * active call screens or backing out when calls are ended).
 */
@Composable
fun CallNavigationHandler(
    navController: NavController,
    callState: CallState,
    callScreenMinimized: MutableState<Boolean>,
) {
    LaunchedEffect(callState, callScreenMinimized.value) {
        val publicKey = callState.publicKeyForCallScreen()
        val route = navController.currentBackStackEntry?.destination?.route
        if (publicKey != null && !callScreenMinimized.value) {
            if (!AppRoutes.isCall(route)) {
                navController.navigateSingleTop(AppRoutes.Call(publicKey))
            }
        } else if (AppRoutes.isCall(route)) {
            navController.popBackStack()
        }
    }
}

/**
 * Intercepts incoming external share streams and redirects the user to forwarding options.
 */
@Composable
fun SharedContentNavigationHandler(
    navController: NavController,
    sharedContent: SharedContent?,
    currentRoute: String?,
) {
    LaunchedEffect(sharedContent, currentRoute) {
        if (sharedContent != null) {
            val isAuthRoute = currentRoute?.endsWith("AppRoutes.Launch") == true ||
                              currentRoute?.endsWith("AppRoutes.Unlock") == true ||
                              currentRoute?.endsWith("AppRoutes.CreateProfile") == true
            if (currentRoute != null && !isAuthRoute) {
                navController.navigate(AppRoutes.ForwardShared) {
                    launchSingleTop = true
                }
            }
        }
    }
}

internal fun CallState.publicKeyForCallScreen(): String? {
    return when (this) {
        is CallState.OutgoingRequesting -> publicKey.string()
        is CallState.OutgoingWaiting -> publicKey.string()
        is CallState.Connecting -> publicKey.string()
        is CallState.OutgoingRinging -> publicKey.string()
        is CallState.Active -> publicKey.string()
        else -> null
    }
}
