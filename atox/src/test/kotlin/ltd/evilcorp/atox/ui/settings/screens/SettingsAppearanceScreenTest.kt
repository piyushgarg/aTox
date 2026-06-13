package ltd.evilcorp.atox.ui.settings.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import ltd.evilcorp.domain.features.settings.model.DateFormatPreference
import ltd.evilcorp.domain.features.settings.model.TimeFormatPreference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class SettingsAppearanceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsAppearanceScreenDisplaysElements() {
        composeTestRule.setContent {
            SettingsAppearanceScreen(
                paddingValues = PaddingValues(0.dp),
                currentLanguageCode = "en-US",
                languages = listOf("en-US" to "English"),
                appThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                timeFormatPreference = TimeFormatPreference.System,
                dateFormatPreference = DateFormatPreference.System,
                dynamicColor = true,
                currentAccentSeed = 0xFF3F51B5.toInt(),
                hapticEnabled = true,
                performHaptic = {},
                onLanguageClick = {},
                onThemeClick = {},
                onDateFormatClick = {},
                onTimeFormatClick = {},
                onDynamicColorChanged = {},
                onAccentColorClick = {},
                onHapticEnabledChanged = {}
            )
        }

        // Ensure it renders without crashing
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.language)).assertIsDisplayed()
    }
}
