package ltd.evilcorp.atox.ui.settings.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import ltd.evilcorp.domain.features.settings.model.FtAutoAccept
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class SettingsChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsChatScreenDisplaysElements() {
        composeTestRule.setContent {
            SettingsChatScreen(
                paddingValues = PaddingValues(0.dp),
                ftAutoAccept = FtAutoAccept.None,
                autoSaveToDownloads = false,
                autoSaveDirectoryLabel = "",
                cacheSizeText = "12 MB",
                enableReplies = true,
                performHaptic = {},
                onFtAutoAcceptClick = {},
                onAutoSaveToDownloadsChanged = {},
                onAutoSaveDirectoryClick = {},
                onClearCacheClick = {},
                onEnableRepliesChanged = {}
            )
        }
        // Ensure it renders without crashing
    }
}
