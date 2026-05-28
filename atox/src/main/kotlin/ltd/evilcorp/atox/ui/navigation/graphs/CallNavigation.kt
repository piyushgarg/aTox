package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.addcontact.AddContactScreen
import ltd.evilcorp.atox.ui.addcontact.AddContactViewModel
import ltd.evilcorp.atox.ui.call.CallScreen
import ltd.evilcorp.atox.ui.call.CallViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.atox.infrastructure.util.PermissionManager
import ltd.evilcorp.domain.core.model.PublicKey

fun NavGraphBuilder.callGraph(
    navController: NavHostController,

    permissionManager: PermissionManager,
    callScreenMinimized: MutableState<Boolean>,
    settings: Settings,
) {
    composable<AppRoutes.Call>(
        enterTransition = { AToxMotion.slideUpEnter() },
        exitTransition = { AToxMotion.slideDownExit() },
        popEnterTransition = { AToxMotion.slideUpEnter() },
        popExitTransition = { AToxMotion.slideDownExit() },
    ) { backStackEntry ->
        val callRoute = backStackEntry.toRoute<AppRoutes.Call>()
        val publicKeyStr = callRoute.publicKey
        val viewModel: CallViewModel = hiltViewModel()

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

    composable<AppRoutes.AddContact>(
        enterTransition = { AToxMotion.slideXEnter(forward = true) },
        exitTransition = { AToxMotion.slideXExit(forward = true) },
        popEnterTransition = { AToxMotion.slideXEnter(forward = false) },
        popExitTransition = { AToxMotion.slideXExit(forward = false) }
    ) { backStackEntry ->
        val addContactRoute = backStackEntry.toRoute<AppRoutes.AddContact>()
        val toxIdArg = addContactRoute.toxId.orEmpty()
        val viewModel: AddContactViewModel = hiltViewModel()
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
