// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ltd.evilcorp.atox.ui.contactprofile.ContactProfileScreen
import ltd.evilcorp.atox.ui.contactprofile.ContactProfileViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.domain.features.contacts.model.ContactCardParser

fun NavGraphBuilder.contactProfileRoute(
    navController: NavHostController
) {
    composable<AppRoutes.ContactProfile>(
        enterTransition = { AToxMotion.slideXEnter(forward = true) },
        exitTransition = { AToxMotion.slideXExit(forward = true) },
        popEnterTransition = { AToxMotion.slideXEnter(forward = false) },
        popExitTransition = { AToxMotion.slideXExit(forward = false) }
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<AppRoutes.ContactProfile>()
        val viewModel: ContactProfileViewModel = hiltViewModel()

        LaunchedEffect(route.publicKey) {
            viewModel.loadContact(route.publicKey)
        }

        val contact by viewModel.contact.collectAsStateWithLifecycle()
        val dateFormatPreference by viewModel.dateFormatPreference.collectAsStateWithLifecycle()
        val timeFormatPreference by viewModel.timeFormatPreference.collectAsStateWithLifecycle()

        ContactProfileScreen(
            contact = contact,
            publicKey = route.publicKey,
            dateFormatPreference = dateFormatPreference,
            timeFormatPreference = timeFormatPreference,
            onBack = { navController.popBackStack() },
            onShareContact = {
                contact?.let { c ->
                    val contactCard = ContactCardParser.encode(
                        toxId = route.publicKey,
                        displayName = c.name
                    )
                    navController.navigate(
                        AppRoutes.ForwardSelection(
                            message = contactCard,
                            messageType = 0,
                            correlationId = 0,
                            isContactShare = true
                        )
                    )
                }
            },
            bottomPadding = 0.dp
        )
    }
}
