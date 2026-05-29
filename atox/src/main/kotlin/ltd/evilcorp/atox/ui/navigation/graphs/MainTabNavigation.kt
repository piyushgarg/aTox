package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.contactlist.ContactListViewModel

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.mainTabGraph(
    navController: NavHostController,
    contactListViewModel: ContactListViewModel,
    settings: Settings,
    appearance: AppAppearance,
    isExpanded: () -> Boolean,
    onThemeChanged: (Int) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onAccentColorSeedChanged: (Int) -> Unit,
    onLocaleTagChanged: (String) -> Unit,
    onDisableScreenshotsChanged: (Boolean) -> Unit,
) {
    chatsTabRoute(navController, contactListViewModel, settings, isExpanded)
    groupsTabRoute(navController, contactListViewModel, isExpanded)
    addContactTabRoute(navController)
    profileTabRoute(navController, settings)
    settingsTabRoute(
        navController = navController,
        settings = settings,
        appearance = appearance,
        onThemeChanged = onThemeChanged,
        onDynamicColorChanged = onDynamicColorChanged,
        onAccentColorSeedChanged = onAccentColorSeedChanged,
        onLocaleTagChanged = onLocaleTagChanged,
        onDisableScreenshotsChanged = onDisableScreenshotsChanged
    )
}
