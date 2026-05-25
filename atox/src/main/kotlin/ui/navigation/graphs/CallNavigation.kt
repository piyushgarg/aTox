package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.ui.addcontact.AddContactScreen
import ltd.evilcorp.atox.ui.addcontact.AddContactViewModel
import ltd.evilcorp.atox.ui.call.CallScreen
import ltd.evilcorp.atox.ui.call.CallViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.atox.util.PermissionManager
import ltd.evilcorp.core.model.PublicKey

fun NavGraphBuilder.callGraph(
    navController: NavHostController,
    vmFactory: ViewModelProvider.Factory,
    permissionManager: PermissionManager,
    callScreenMinimized: MutableState<Boolean>,
    settings: Settings,
) {
    composable(
        route = AppRoutes.Call,
        arguments = listOf(navArgument(AppRoutes.PublicKeyArg) { type = NavType.StringType }),
        enterTransition = { AToxMotion.slideUpEnter() },
        exitTransition = { AToxMotion.slideDownExit() },
        popEnterTransition = { AToxMotion.slideUpEnter() },
        popExitTransition = { AToxMotion.slideDownExit() },
    ) { backStackEntry ->
        val publicKeyStr = backStackEntry.arguments?.getString(AppRoutes.PublicKeyArg).orEmpty()
        val viewModel: CallViewModel = viewModel(factory = vmFactory)

        androidx.compose.runtime.LaunchedEffect(publicKeyStr) {
            viewModel.setActiveContact(PublicKey(publicKeyStr))
            callScreenMinimized.value = false
        }

        val contact by viewModel.contact.collectAsStateWithLifecycle()
        val callState by viewModel.inCall.collectAsStateWithLifecycle()
        val sendingAudio by viewModel.sendingAudio.collectAsStateWithLifecycle()
        val speakerphoneOn by viewModel.speakerphoneState.collectAsStateWithLifecycle()
        val callDuration by viewModel.callDuration.collectAsStateWithLifecycle()

        CallScreen(
            publicKey = publicKeyStr,
            contact = contact,
            callState = callState,
            sendingAudio = sendingAudio,
            speakerphoneOn = speakerphoneOn,
            callDuration = callDuration,
            hasMicPermission = permissionManager.canRecordAudio(),
            onRequestMicPermission = {},
            onMinimize = {
                callScreenMinimized.value = true
                navController.popBackStack()
            },
            onToggleMic = {
                if (sendingAudio) {
                    viewModel.stopSendingAudio()
                } else {
                    viewModel.startSendingAudio()
                }
            },
            onToggleSpeaker = viewModel::toggleSpeakerphone,
            onEndCall = {
                callScreenMinimized.value = false
                viewModel.endCall()
                navController.popBackStack()
            },
            hapticEnabled = settings.hapticEnabled,
        )
    }

    composable(
        route = AppRoutes.AddContact,
        arguments = listOf(navArgument(AppRoutes.ToxIdArg) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }),
    ) { backStackEntry ->
        val toxIdArg = backStackEntry.arguments?.getString(AppRoutes.ToxIdArg).orEmpty()
        val viewModel: AddContactViewModel = viewModel(factory = vmFactory)
        AddContactScreen(
            viewModel = viewModel,
            initialToxId = toxIdArg,
            onBack = navController::popBackStack,
            onSuccess = {
                navController.popBackStack()
            }
        )
    }
}
