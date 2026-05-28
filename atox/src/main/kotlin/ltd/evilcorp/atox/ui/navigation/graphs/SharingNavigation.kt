// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.navigation.graphs

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.SharedContent
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.chat.ForwardSelectionScreen
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.theme.AToxMotion

fun NavGraphBuilder.sharingGraph(
    navController: NavHostController,
    contactListViewModel: ContactListViewModel,
    settings: Settings,
) {
    // Forward selection
    composable<AppRoutes.ForwardSelection>(
        enterTransition = { AToxMotion.slideXEnter(forward = true) },
        exitTransition = { AToxMotion.slideXExit(forward = true) },
        popEnterTransition = { AToxMotion.slideXEnter(forward = false) },
        popExitTransition = { AToxMotion.slideXExit(forward = false) },
    ) { backStackEntry ->
        val forwardRoute = backStackEntry.toRoute<AppRoutes.ForwardSelection>()
        val messageText = forwardRoute.message
        val contactsState by contactListViewModel.contacts.collectAsStateWithLifecycle()
        val ctx = LocalContext.current

        ForwardSelectionScreen(
            contacts = contactsState,
            settings = settings,
            onBack = { navController.popBackStack() },
            onContactsSelect = { selectedList ->
                selectedList.forEach { contact ->
                    contactListViewModel.onShareText(messageText, contact)
                }
                Toast.makeText(ctx, ctx.getString(R.string.message_forwarded), Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        )
    }

    // Shared content forwarding
    composable<AppRoutes.ForwardShared>(
        enterTransition = { AToxMotion.slideXEnter(forward = true) },
        exitTransition = { AToxMotion.slideXExit(forward = true) },
        popEnterTransition = { AToxMotion.slideXEnter(forward = false) },
        popExitTransition = { AToxMotion.slideXExit(forward = false) },
    ) {
        val contactsState by contactListViewModel.contacts.collectAsStateWithLifecycle()
        val sharedContent by contactListViewModel.sharedContent.collectAsStateWithLifecycle()
        val ctx = LocalContext.current

        ForwardSelectionScreen(
            contacts = contactsState,
            settings = settings,
            onBack = {
                contactListViewModel.clearSharedContent()
                navController.popBackStack()
            },
            onContactsSelect = { selectedList ->
                val content = sharedContent
                if (content != null) {
                    selectedList.forEach { contact ->
                        when (content) {
                            is SharedContent.Text -> {
                                contactListViewModel.onShareText(content.text, contact)
                            }
                            is SharedContent.File -> {
                                contactListViewModel.onShareFile(content.uri, contact)
                            }
                            is SharedContent.MultipleFiles -> {
                                content.uris.forEach { uri ->
                                    contactListViewModel.onShareFile(uri, contact)
                                }
                            }
                        }
                    }
                    if (content is SharedContent.Text) {
                        Toast.makeText(ctx, ctx.getString(R.string.message_forwarded), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, ctx.getString(R.string.file_sharing_started), Toast.LENGTH_SHORT).show()
                    }
                    contactListViewModel.clearSharedContent()
                }
                navController.popBackStack()
                if (selectedList.size == 1) {
                    navController.navigate(AppRoutes.Chat(selectedList.first().publicKey)) {
                        popUpTo(AppRoutes.Chats) { inclusive = false }
                    }
                }
            }
        )
    }
}
