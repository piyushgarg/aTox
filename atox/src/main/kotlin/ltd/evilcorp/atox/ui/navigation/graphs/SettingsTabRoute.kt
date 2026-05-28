package ltd.evilcorp.atox.ui.navigation.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.settings.SettingsScreen
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.navigation.LocalTabPadding
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.settingsTabRoute(
    navController: NavHostController,
    settings: Settings,
    appearance: AppAppearance,
    onThemeChanged: (Int) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onAccentColorSeedChanged: (Int) -> Unit,
    onLocaleTagChanged: (String) -> Unit,
    onDisableScreenshotsChanged: (Boolean) -> Unit
) {
    composable<AppRoutes.Settings> {
        val context = LocalContext.current
        val settingsTitleState = remember { mutableStateOf("") }
        val settingsOnBackActionState = remember { mutableStateOf<(() -> Unit)?>(null) }
        val settingsOnSearchActionState = remember { mutableStateOf<(() -> Unit)?>(null) }

        val settingsTitle = settingsTitleState.value
        val settingsOnBackAction = settingsOnBackActionState.value
        val settingsOnSearchAction = settingsOnSearchActionState.value

        val settingsViewModel: ltd.evilcorp.atox.ui.settings.SettingsViewModel = hiltViewModel()
        val user by settingsViewModel.user.collectAsStateWithLifecycle()
        val connectionStatus = user?.connectionStatus ?: ConnectionStatus.None

        Scaffold(
            topBar = {
                if (settingsTitle != context.getString(R.string.search_settings)) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (settingsOnBackAction == null) context.getString(R.string.settings) else settingsTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (connectionStatus == ConnectionStatus.None) {
                                    Text(
                                        text = context.getString(R.string.connecting),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (settingsOnBackAction != null) {
                                IconButton(onClick = { settingsOnBackAction.invoke() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = context.getString(R.string.navigation_back)
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.padding(start = 4.dp)) {
                                    ltd.evilcorp.atox.ui.common.MorphingNavigationIcon(
                                        isBack = false,
                                        onClick = { settingsOnSearchAction?.invoke() }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SettingsScreen(
                    settings = settings,
                    appearance = appearance,
                    onThemeChanged = onThemeChanged,
                    onDynamicColorChanged = onDynamicColorChanged,
                    onAccentColorSeedChanged = onAccentColorSeedChanged,
                    onLocaleTagChanged = onLocaleTagChanged,
                    onDisableScreenshotsChanged = onDisableScreenshotsChanged,
                    showBackButton = false,

                    onTitleChanged = { settingsTitleState.value = it },
                    onBackActionChanged = { settingsOnBackActionState.value = it },
                    onSearchActionChanged = { action ->
                        settingsOnSearchActionState.value = if (action != null) {
                            { navController.navigate(AppRoutes.SearchSettings) }
                        } else null
                    }
                )
            }
        }
    }
}
