package ltd.evilcorp.atox.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.settings.backup.BackupSettingsViewModel
import ltd.evilcorp.atox.ui.settings.backup.BackupUiEvent
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.settings.model.ProxyStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsScreenDisplaysRootCategories() {
        val mockSettings = mockk<Settings>(relaxed = true)
        every { mockSettings.state } returns MutableStateFlow(ltd.evilcorp.domain.features.settings.model.UserSettings())
        
        val appAppearance = AppAppearance(
            themeMode = -1,
            dynamicColorEnabled = false,
            accentColorSeed = 0,
            localeTag = "en-US"
        )

        val mockViewModel = mockk<SettingsViewModel>(relaxed = true)
        every { mockViewModel.user } returns MutableStateFlow<User?>(null)
        every { mockViewModel.showProxyDialog } returns MutableStateFlow(false)
        every { mockViewModel.showFtAcceptDialog } returns MutableStateFlow(false)
        every { mockViewModel.showBootstrapDialog } returns MutableStateFlow(false)
        every { mockViewModel.proxyStatus } returns MutableStateFlow<ProxyStatus?>(null)
        every { mockViewModel.uiEvents } returns MutableSharedFlow<SettingsUiEvent>()
        every { mockViewModel.committed } returns MutableStateFlow(false)

        val mockBackupViewModel = mockk<BackupSettingsViewModel>(relaxed = true)
        every { mockBackupViewModel.backupExporting } returns MutableStateFlow(false)
        every { mockBackupViewModel.backupImporting } returns MutableStateFlow(false)
        every { mockBackupViewModel.uiEvents } returns MutableSharedFlow<BackupUiEvent>()
        every { mockBackupViewModel.backupProviders } returns emptyList()
        every { mockBackupViewModel.googleBackups } returns MutableStateFlow(emptyList())

        composeTestRule.setContent {
            SettingsScreen(
                settings = mockSettings,
                appearance = appAppearance,
                onThemeChanged = {},
                onDynamicColorChanged = {},
                onAccentColorSeedChanged = {},
                onLocaleTagChanged = {},
                onDisableScreenshotsChanged = {},
                viewModel = mockViewModel,
                backupViewModel = mockBackupViewModel
            )
        }

        // Check root categories are displayed
        composeTestRule.onNodeWithText("Appearance & Design").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("File Transfer").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Network & Connection").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Backup").performScrollTo().assertIsDisplayed()
    }
}
