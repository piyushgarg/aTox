package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ltd.evilcorp.atox.ui.createprofile.CreateProfileScreen
import ltd.evilcorp.atox.ui.createprofile.CreateProfileViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.navigation.AuthViewModel
import ltd.evilcorp.atox.ui.navigation.LaunchScreen
import ltd.evilcorp.atox.ui.navigation.UnlockScreen
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onQuitApp: () -> Unit,
) {
    composable<AppRoutes.Launch> {
        val authViewModel: AuthViewModel = hiltViewModel()
        LaunchScreen(
            viewModel = authViewModel,
            onLaunchResolved = { status ->
                val target: Any = when (status) {
                    ToxSaveStatus.Ok -> AppRoutes.Chats
                    ToxSaveStatus.Encrypted -> AppRoutes.Unlock
                    else -> AppRoutes.CreateProfile
                }
                navController.navigate(target) {
                    popUpTo(AppRoutes.Launch) { inclusive = true }
                }
            }
        )
    }

    composable<AppRoutes.Unlock> {
        val authViewModel: AuthViewModel = hiltViewModel()
        UnlockScreen(
            viewModel = authViewModel,
            onUnlockSuccess = {
                navController.navigate(AppRoutes.Chats) {
                    popUpTo(AppRoutes.Unlock) { inclusive = true }
                }
            },
            onQuit = onQuitApp
        )
    }

    composable<AppRoutes.CreateProfile> {
        val viewModel: CreateProfileViewModel = hiltViewModel()
        CreateProfileScreen(
            viewModel = viewModel,
            onSuccess = {
                navController.navigate(AppRoutes.Chats) {
                    popUpTo(AppRoutes.CreateProfile) { inclusive = true }
                }
            }
        )
    }
}
