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
    data object ProfilePicker : AppRoutes

    @Serializable
    data object Unlock : AppRoutes

    @Serializable
    data object Welcome : AppRoutes

    @Serializable
    data object Chats : AppRoutes

    @Serializable
    data object AddContactTab : AppRoutes

    @Serializable
    data object Profile : AppRoutes

    @Serializable
    data object Settings : AppRoutes

    @Serializable
    data object CreateProfile : AppRoutes

    @Serializable
    data object RestoreBackup : AppRoutes

    @Serializable
    data class Chat(val publicKey: String) : AppRoutes

    @Serializable
    data class Call(val publicKey: String) : AppRoutes

    @Serializable
    data class AddContact(val toxId: String? = null) : AppRoutes

    @Serializable
    data class ContactProfile(val publicKey: String) : AppRoutes

    @Serializable
    data class ForwardSelection(
        val message: String,
        val messageType: Int = 0,
        val correlationId: Int = 0,
        val isContactShare: Boolean = false
    ) : AppRoutes

    @Serializable
    data object ForwardShared : AppRoutes

    @Serializable
    data class GroupChat(val chatId: String) : AppRoutes

    @Serializable
    data object CreateGroup : AppRoutes

    @Serializable
    data object JoinGroup : AppRoutes

    @Serializable
    data object SearchContacts : AppRoutes

    @Serializable
    data object SearchSettings : AppRoutes

    companion object {
        fun isMainTab(route: String?) = route != null && (
            route.endsWith("AppRoutes.Chats") ||
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
