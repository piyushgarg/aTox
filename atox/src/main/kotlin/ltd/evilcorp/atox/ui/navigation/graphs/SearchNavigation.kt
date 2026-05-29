package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.ui.theme.AToxMotion
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel
import ltd.evilcorp.atox.ui.navigation.AppRoutes

fun NavGraphBuilder.searchGraph(
    navController: NavController,
    contactListViewModel: ContactListViewModel,
    settings: Settings,
    appearance: AppAppearance
) {
    composable<AppRoutes.SearchContacts>(
        enterTransition = { AToxMotion.slideXEnter(forward = true) },
        exitTransition = { AToxMotion.slideXExit(forward = true) },
        popEnterTransition = { AToxMotion.slideXEnter(forward = false) },
        popExitTransition = { AToxMotion.slideXExit(forward = false) }
    ) {
        val contacts by contactListViewModel.contacts.collectAsStateWithLifecycle()
        ltd.evilcorp.atox.ui.contactlist.SearchContactsScreen(
            contacts = contacts,
            onContactClick = { contact ->
                contactListViewModel.prepareOpenChat(contact)
                navController.navigate(AppRoutes.Chat(contact.publicKey)) {
                    popUpTo(AppRoutes.SearchContacts) { inclusive = true }
                }
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable<AppRoutes.SearchSettings>(
        enterTransition = { AToxMotion.slideXEnter(forward = true) },
        exitTransition = { AToxMotion.slideXExit(forward = true) },
        popEnterTransition = { AToxMotion.slideXEnter(forward = false) },
        popExitTransition = { AToxMotion.slideXExit(forward = false) }
    ) {
        ltd.evilcorp.atox.ui.settings.SearchSettingsScreen(
            settings = settings,
            appearance = appearance,
            onItemClick = { item ->
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }
}
