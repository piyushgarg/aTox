package ltd.evilcorp.atox.ui.navigation

import kotlinx.serialization.Serializable
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Suppress("CompositionLocalAllowlist")
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

@Suppress("CompositionLocalAllowlist")
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

sealed interface AppRoutes {
    @Serializable
    data object Launch : AppRoutes

    @Serializable
    data object Unlock : AppRoutes

    @Serializable
    data object Chats : AppRoutes

    @Serializable
    data object Groups : AppRoutes

    @Serializable
    data object AddContactTab : AppRoutes

    @Serializable
    data object Profile : AppRoutes

    @Serializable
    data object Settings : AppRoutes

    @Serializable
    data object CreateProfile : AppRoutes

    @Serializable
    data class Chat(val publicKey: String) : AppRoutes

    @Serializable
    data class Call(val publicKey: String) : AppRoutes

    @Serializable
    data class AddContact(val toxId: String? = null) : AppRoutes

    @Serializable
    data class ForwardSelection(val message: String) : AppRoutes

    @Serializable
    data object ForwardShared : AppRoutes

    @Serializable
    data class GroupChat(val chatId: String) : AppRoutes

    @Serializable
    data object CreateGroup : AppRoutes

    @Serializable
    data object JoinGroup : AppRoutes

    companion object {
        fun isMainTab(route: String?) = route != null && (
            route.endsWith("AppRoutes.Chats") ||
            route.endsWith("AppRoutes.Groups") ||
            route.endsWith("AppRoutes.AddContactTab") ||
            route.endsWith("AppRoutes.Profile") ||
            route.endsWith("AppRoutes.Settings")
        )

        fun isCall(route: String?) = route != null && route.contains("AppRoutes.Call")
    }
}

fun <T : Any> androidx.navigation.NavController.navigateSingleTop(route: T) {
    navigate(route) {
        launchSingleTop = true
    }
}
