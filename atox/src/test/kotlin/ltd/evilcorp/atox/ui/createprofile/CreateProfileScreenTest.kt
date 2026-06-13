package ltd.evilcorp.atox.ui.createprofile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.github.takahirom.roborazzi.captureRoboImage

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class CreateProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCreateProfileScreenInitialState() {
        composeTestRule.setContent {
            CreateProfileContent(
                isLoading = false,
                errorText = ""
            )
        }

        // Welcome text should be displayed
        composeTestRule.onNodeWithText("Welcome to aTox!").assertIsDisplayed()
        
        // Name input should be empty, so Create Profile button should be disabled
        composeTestRule.onNodeWithText("Create Profile").assertIsNotEnabled()
    }

    @Test
    fun testCreateProfileButtonBecomesEnabledWhenNameEntered() {
        composeTestRule.setContent {
            CreateProfileContent(
                isLoading = false,
                errorText = ""
            )
        }

        composeTestRule.onNodeWithText("Username").performTextInput("Satoshi")
        composeTestRule.onNodeWithText("Create Profile").assertIsEnabled()
    }

    @Test
    fun testCreateProfileButtonTriggersCallback() {
        var clickedName = ""
        
        composeTestRule.setContent {
            CreateProfileContent(
                isLoading = false,
                errorText = "",
                onCreateProfile = { clickedName = it }
            )
        }

        composeTestRule.onNodeWithText("Username").performTextInput("Alice")
        composeTestRule.onNodeWithText("Create Profile").performClick()
        
        assertEquals("Alice", clickedName)
    }

    @Test
    fun testErrorTextIsDisplayed() {
        composeTestRule.setContent {
            CreateProfileContent(
                isLoading = false,
                errorText = "Name cannot be empty!"
            )
        }

        composeTestRule.onNodeWithText("Name cannot be empty!").assertIsDisplayed()
    }

    @Test
    fun testInputsAreDisabledWhenLoading() {
        composeTestRule.setContent {
            CreateProfileContent(
                isLoading = true,
                errorText = ""
            )
        }

        // The text fields should be disabled when loading
        composeTestRule.onNodeWithText("Username").assertIsNotEnabled()
        // Text is removed from the primary AtoxLoadingButton when loading
        composeTestRule.onNode(
            androidx.compose.ui.test.hasText("Restore from backup", ignoreCase = true) and 
            androidx.compose.ui.test.hasClickAction()
        ).assertIsNotEnabled()
        composeTestRule.onNode(
            androidx.compose.ui.test.hasText("Restore from Google Drive", ignoreCase = true) and 
            androidx.compose.ui.test.hasClickAction()
        ).assertIsNotEnabled()
        composeTestRule.onNodeWithText("Create Profile").assertDoesNotExist()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun createProfileScreen_layout_matchesSnapshot() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                CreateProfileContent(
                    isLoading = false,
                    errorText = ""
                )
            }
        }
        
        composeTestRule.onNode(androidx.compose.ui.test.isRoot()).captureRoboImage(
            filePath = "src/test/snapshots/images/create_profile_screen_default.png"
        )
    }

    @Test
    fun testCreateProfileScreen_ux_rapidClicks() {
        var createCount = 0
        
        composeTestRule.setContent {
            CreateProfileContent(
                isLoading = false,
                errorText = "",
                onCreateProfile = { createCount++ }
            )
        }

        val nameInput = composeTestRule.onNodeWithText("Username")
        nameInput.performTextInput("Alice")
        
        val createButton = composeTestRule.onNodeWithText("Create Profile")
        repeat(10) {
            createButton.performClick()
        }
        
        assertEquals(10, createCount)
    }
}
