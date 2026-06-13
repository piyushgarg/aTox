package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    composable<AppRoutes.Launch>(
        exitTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXExit(forward = true) },
        popEnterTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXEnter(forward = false) }
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val showPicker = androidx.compose.runtime.remember { ltd.evilcorp.core.profile.ProfileManager.getShowProfilePicker(context) }
        if (showPicker) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                navController.navigate(AppRoutes.ProfilePicker) {
                    popUpTo(AppRoutes.Launch) { inclusive = true }
                }
            }
        } else {
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
    }

    composable<AppRoutes.ProfilePicker>(
        enterTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXEnter(forward = true) },
        exitTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXExit(forward = true) },
        popEnterTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXEnter(forward = false) },
        popExitTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXExit(forward = false) }
    ) {
        ltd.evilcorp.atox.ui.profilepicker.ProfilePickerScreen(
            onProfileSelected = {
                val context = navController.context
                cleanupBeforeSwitch(context)
                val entryPoint = dagger.hilt.EntryPoints.get(
                    context.applicationContext,
                    ToxEntryPoint::class.java
                )
                val disableRestart = entryPoint.disableRestart()
                if (disableRestart) {
                    entryPoint.toxStarter().tryLoadTox(null)
                    navController.navigate(AppRoutes.Chats) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                } else {
                    val status = entryPoint.toxStarter().tryLoadTox(null)
                    val target: Any = when (status) {
                        ToxSaveStatus.Ok -> AppRoutes.Chats
                        ToxSaveStatus.Encrypted -> AppRoutes.Unlock
                        else -> AppRoutes.CreateProfile
                    }
                    navController.navigate(target) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            },
            onCreateProfile = {
                val context = navController.context
                val newId = java.util.UUID.randomUUID().toString()
                ltd.evilcorp.core.profile.ProfileManager.setActiveProfileId(context, newId)
                ltd.evilcorp.core.profile.ProfileManager.setShowProfilePicker(context, false)
                cleanupBeforeSwitch(context)
                navController.navigate(AppRoutes.CreateProfile) {
                    popUpTo(AppRoutes.ProfilePicker) { inclusive = true }
                }
            }
        )
    }

    composable<AppRoutes.Unlock>(
        enterTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXEnter(forward = true) },
        exitTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXExit(forward = true) },
        popEnterTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXEnter(forward = false) },
        popExitTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXExit(forward = false) }
    ) {
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

    composable<AppRoutes.CreateProfile>(
        enterTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXEnter(forward = true) },
        exitTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXExit(forward = true) },
        popEnterTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXEnter(forward = false) },
        popExitTransition = { ltd.evilcorp.atox.ui.theme.AToxMotion.sharedAxisXExit(forward = false) }
    ) {
        val viewModel: CreateProfileViewModel = hiltViewModel()
        CreateProfileScreen(
            viewModel = viewModel,
            onSuccess = {
                navController.navigate(AppRoutes.Chats) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        )
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface ToxEntryPoint {
    fun tox(): ltd.evilcorp.domain.core.network.ITox
    fun dbProvider(): ltd.evilcorp.core.db.ProfileDatabaseProvider
    fun toxStarter(): ltd.evilcorp.domain.core.network.IToxStarter
    @ltd.evilcorp.atox.DisableRestart fun disableRestart(): Boolean
}

private const val MAX_WAIT_ITERATIONS = 100
private const val WAIT_ITERATION_DELAY_MS = 20L

private fun cleanupBeforeSwitch(context: android.content.Context) {
    try {
        context.stopService(android.content.Intent(context, ltd.evilcorp.atox.infrastructure.service.ToxService::class.java))
        val entryPoint = dagger.hilt.EntryPoints.get(
            context.applicationContext,
            ToxEntryPoint::class.java
        )
        val tox = entryPoint.tox()
        tox.stop()
        if (tox is ltd.evilcorp.core.tox.ToxImpl) {
            tox.waitForStop()
        } else {
            var waitCount = 0
            while (tox.started && waitCount < MAX_WAIT_ITERATIONS) {
                Thread.sleep(WAIT_ITERATION_DELAY_MS)
                waitCount++
            }
        }
        entryPoint.dbProvider().closeDatabase()
    } catch (e: Exception) {
        android.util.Log.e("AuthNavigation", "Error cleaning up before switch", e)
    }
}
