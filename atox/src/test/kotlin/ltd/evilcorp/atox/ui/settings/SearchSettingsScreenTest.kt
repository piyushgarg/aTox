package ltd.evilcorp.atox.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import ltd.evilcorp.atox.appearance.AppAppearance
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.settings.common.SearchableSetting
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class SearchSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSearchSettingsScreen_filtersAndClicksItem() {
        val mockSettings = mockk<Settings>(relaxed = true)
        every { mockSettings.state } returns MutableStateFlow(ltd.evilcorp.domain.features.settings.model.UserSettings())

        val appAppearance = AppAppearance(
            themeMode = -1,
            dynamicColorEnabled = false,
            accentColorSeed = 0,
            localeTag = "en-US"
        )

        val mockViewModel = mockk<SettingsViewModel>(relaxed = true)
        every { mockViewModel.showProxyDialog } returns MutableStateFlow(false)
        every { mockViewModel.showFtAcceptDialog } returns MutableStateFlow(false)
        every { mockViewModel.showBootstrapDialog } returns MutableStateFlow(false)
        every { mockViewModel.uiEvents } returns MutableSharedFlow<SettingsUiEvent>()
        every { mockViewModel.committed } returns MutableStateFlow(false)

        var clickedItem: SearchableSetting? = null

        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                SearchSettingsScreen(
                    settings = mockSettings,
                    appearance = appAppearance,
                    onItemClick = { clickedItem = it },
                    onBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Initially lists all or none depending on empty search query (SettingsSearchPopup usually shows all or filters).
        // Let's type a search query: "Theme" or "Sounds"
        // Under English locale, "Sounds" matches the sound setting (R.string.settings_sounds_group)
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Sound")

        // Wait/assert "Sounds" is displayed
        composeTestRule.onNodeWithText("Sounds").assertIsDisplayed()

        // "Backup" shouldn't be visible when filtered by "Sound"
        composeTestRule.onNodeWithText("Backup").assertDoesNotExist()

        // Click on "Sounds"
        composeTestRule.onNodeWithText("Sounds").performClick()

        // Verify click callback received correct item
        assertEquals("Sounds", clickedItem?.title)
    }
}
