package ltd.evilcorp.atox.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.github.takahirom.roborazzi.captureRoboImage
import ltd.evilcorp.atox.ui.navigation.components.UnlockScreenContent

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class LaunchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unlockScreen_buttonSubmitsPassword() {
        var submittedPassword = ""
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                UnlockScreenContent(
                    isError = false,
                    isLoading = false,
                    isBiometricEnabled = false,
                    onSubmitUnlock = { password -> submittedPassword = password },
                    onQuit = {},
                    onClearError = {},
                    onBiometricClick = {}
                )
            }
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("my_secret_password")
        composeTestRule.onNode(hasText("Unlock") or hasText("Разблокировать")).performClick()

        assert(submittedPassword == "my_secret_password")
    }

    @Test
    fun unlockScreen_displaysError() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                UnlockScreenContent(
                    isError = true,
                    isLoading = false,
                    isBiometricEnabled = false,
                    onSubmitUnlock = {},
                    onQuit = {},
                    onClearError = {},
                    onBiometricClick = {}
                )
            }
        }

        // Just check if error text is displayed (e.g. Invalid password)
        composeTestRule.onNode(hasText("Invalid password") or hasText("Неверный пароль")).assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun unlockScreen_layout_matchesSnapshot() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                UnlockScreenContent(
                    isError = false,
                    isLoading = false,
                    isBiometricEnabled = false,
                    onSubmitUnlock = {},
                    onQuit = {},
                    onClearError = {},
                    onBiometricClick = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/unlock_screen_default.png"
        )
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun launchTimeoutScreen_layout_matchesSnapshot() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                LaunchTimeoutScreen(
                    onRetry = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/launch_timeout_screen_default.png"
        )
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun launchScreen_layout_matchesSnapshot() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                LaunchScreenContent()
            }
        }
        
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/launch_screen_default.png"
        )
    }
}
