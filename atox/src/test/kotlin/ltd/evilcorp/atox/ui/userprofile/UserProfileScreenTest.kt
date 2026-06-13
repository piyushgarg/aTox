@file:Suppress("MaxLineLength")
package ltd.evilcorp.atox.ui.userprofile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createComposeRule as createComposeRuleV2
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.github.takahirom.roborazzi.captureRoboImage

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class UserProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRuleV2()

    @Test
    fun testUserProfileScreenDisplaysUserData() {
        val testUser = User(
            publicKey = "ABCDEF",
            name = "Satoshi",
            statusMessage = "Hello World",
            status = UserStatus.None
        )

        composeTestRule.setContent {
            UserProfileScreen(
                user = testUser,
                toxId = "TOX_ID_123",
                selfAvatarBitmap = null,
                selectedImageUri = null,
                onSelectedImageUriChanged = {},
                onLaunchCamera = {},
                onLaunchGallery = {},
                onSetName = {},
                onSetStatusMessage = {},
                onSetStatus = {},
                onCropAndSaveAvatar = { _, _, _, _, _, _ -> }
            )
        }

        // We check for the text fields containing the user's name and status
        composeTestRule.onNodeWithText("Satoshi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello World").assertIsDisplayed()
        
        // We check the tox id is displayed
        composeTestRule.onNodeWithText("TOX_ID_123").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testUserProfileScreenCallsCallbacksOnInput() {
        val testUser = User(
            publicKey = "ABCDEF",
            name = "Satoshi",
            statusMessage = "Hello World",
            status = UserStatus.None
        )

        var newName = ""
        var newStatusMessage = ""

        composeTestRule.setContent {
            UserProfileScreen(
                user = testUser,
                toxId = "TOX_ID_123",
                selfAvatarBitmap = null,
                selectedImageUri = null,
                onSelectedImageUriChanged = {},
                onLaunchCamera = {},
                onLaunchGallery = {},
                onSetName = { newName = it },
                onSetStatusMessage = { newStatusMessage = it },
                onSetStatus = {},
                onCropAndSaveAvatar = { _, _, _, _, _, _ -> }
            )
        }

        // Change name
        composeTestRule.onNodeWithText("Satoshi").performTextReplacement("Satoshi Nakamoto")
        assertEquals("Satoshi Nakamoto", newName)

        // Change status message
        composeTestRule.onNodeWithText("Hello World").performTextReplacement("Hello World!")
        assertEquals("Hello World!", newStatusMessage)
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun userProfileScreen_layout_matchesSnapshot() {
        val testUser = User(
            publicKey = "ABCDEF",
            name = "Satoshi",
            statusMessage = "Hello World",
            status = UserStatus.None
        )

        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                UserProfileScreen(
                    user = testUser,
                    toxId = "TOX_ID_1234567890_THIS_IS_A_VERY_LONG_TOX_ID_THAT_SHOULD_BE_DISPLAYED_CORRECTLY",
                    selfAvatarBitmap = null,
                    selectedImageUri = null,
                    onSelectedImageUriChanged = {},
                    onLaunchCamera = {},
                    onLaunchGallery = {},
                    onSetName = {},
                    onSetStatusMessage = {},
                    onSetStatus = {},
                    onCropAndSaveAvatar = { _, _, _, _, _, _ -> }
                )
            }
        }
        
        composeTestRule.onNode(androidx.compose.ui.test.isRoot()).captureRoboImage(
            filePath = "src/test/snapshots/images/user_profile_screen_default.png"
        )
    }

    @Test
    fun userProfileScreen_longNameTextOverflow_matchesSnapshot() {
        val testUser = User(
            publicKey = "ABCDEF",
            name = "Satoshi Nakamoto But With A Very Long Name That Will Surely Overflow The Screen Boundaries And Be Truncated Or Wrapped Dependending On The Setting That We Test Right Now Here",
            statusMessage = "Hello World",
            status = UserStatus.None
        )

        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                UserProfileScreen(
                    user = testUser,
                    toxId = "TOX_ID_1234567890",
                    selfAvatarBitmap = null,
                    selectedImageUri = null,
                    onSelectedImageUriChanged = {},
                    onLaunchCamera = {},
                    onLaunchGallery = {},
                    onSetName = {},
                    onSetStatusMessage = {},
                    onSetStatus = {},
                    onCropAndSaveAvatar = { _, _, _, _, _, _ -> }
                )
            }
        }
        
        composeTestRule.onNode(androidx.compose.ui.test.isRoot()).captureRoboImage(
            filePath = "src/test/snapshots/images/user_profile_screen_long_name.png"
        )
    }

    @Test
    fun userProfileScreen_ux_extremeLongInput_and_rapidClicks() {
        var nameChanges = 0
        val testUser = User(
            publicKey = "ABCDEF",
            name = "Satoshi",
            statusMessage = "Hello World",
            status = UserStatus.None
        )

        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                UserProfileScreen(
                    user = testUser,
                    toxId = "TOX_ID_1234567890",
                    selfAvatarBitmap = null,
                    selectedImageUri = null,
                    onSelectedImageUriChanged = {},
                    onLaunchCamera = {},
                    onLaunchGallery = {},
                    onSetName = { nameChanges++ },
                    onSetStatusMessage = {},
                    onSetStatus = {},
                    onCropAndSaveAvatar = { _, _, _, _, _, _ -> }
                )
            }
        }

        // 1. Extreme long input test
        val longString = "A".repeat(5000)
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasSetTextAction())[0].performTextReplacement(longString)
        
        // Rapid edits (stress test for inputs)
        for (i in 1..10) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasSetTextAction())[0].performTextReplacement("Alice $i")
        }
        
        // Assert that callbacks were triggered
        assertEquals(11, nameChanges) // 1 initial + 10 rapid
    }
}
