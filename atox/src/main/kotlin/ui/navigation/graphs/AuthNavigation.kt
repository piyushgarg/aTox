package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ltd.evilcorp.atox.ui.createprofile.CreateProfileScreen
import ltd.evilcorp.atox.ui.createprofile.CreateProfileViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.navigation.AuthViewModel
import ltd.evilcorp.atox.ui.navigation.LaunchScreen
import ltd.evilcorp.atox.ui.navigation.UnlockScreen
import ltd.evilcorp.core.tox.save.ToxSaveStatus

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    vmFactory: ViewModelProvider.Factory,
    onQuitApp: () -> Unit,
) {
    composable(AppRoutes.Launch) {
        LaunchScreen(
            viewModel = authViewModel,
            onLaunchResolved = { status ->
                val target = when (status) {
                    ToxSaveStatus.Ok -> AppRoutes.ContactList
                    ToxSaveStatus.Encrypted -> AppRoutes.Unlock
                    else -> AppRoutes.CreateProfile
                }
                navController.navigate(target) {
                    popUpTo(AppRoutes.Launch) { inclusive = true }
                }
            }
        )
    }

    composable(AppRoutes.Unlock) {
        UnlockScreen(
            viewModel = authViewModel,
            onUnlockSuccess = {
                navController.navigate(AppRoutes.ContactList) {
                    popUpTo(AppRoutes.Unlock) { inclusive = true }
                }
            },
            onQuit = onQuitApp
        )
    }

    composable(AppRoutes.CreateProfile) {
        val viewModel: CreateProfileViewModel = viewModel(factory = vmFactory)
        CreateProfileScreen(
            viewModel = viewModel,
            onSuccess = {
                navController.navigate(AppRoutes.ContactList) {
                    popUpTo(AppRoutes.CreateProfile) { inclusive = true }
                }
            }
        )
    }
}
