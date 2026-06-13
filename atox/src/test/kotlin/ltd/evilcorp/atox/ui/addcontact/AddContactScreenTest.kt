package ltd.evilcorp.atox.ui.addcontact

import androidx.compose.ui.test.junit4.v2.createComposeRule as createComposeRuleV2
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assert
import ltd.evilcorp.atox.ui.testutils.LayoutMatchers.hasMinTouchTargetSize
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.github.takahirom.roborazzi.captureRoboImage

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class AddContactScreenTest {

    @get:Rule
    val composeTestRule = createComposeRuleV2()

    @Test
    fun addContactFormSubmitsCorrectly() {
        var submittedToxId = ""
        var submittedMessage = ""

        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                AddContactContent(
                    initialToxId = "",
                    showBackButton = true,
                    isLoading = false,
                    errorText = "",
                    onErrorTextChanged = {},
                    onBack = {},
                    onAddContact = { toxId, message ->
                        submittedToxId = toxId
                        submittedMessage = message
                    }
                )
            }
        }
        
        val validToxId = "0000000000000000000000000000000000000000000000000000000000000000000000000000"
        composeTestRule.onNodeWithContentDescription("Tox ID Input").performTextInput(validToxId)
        composeTestRule.onNodeWithContentDescription("Message Input").performTextInput("Hello, please add me!")

        composeTestRule.onNodeWithText("Add").performClick()

        assert(submittedToxId == validToxId)
        assert(submittedMessage.isNotEmpty())
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun addContactScreen_layout_matchesSnapshot() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                AddContactContent(
                    initialToxId = "",
                    showBackButton = true,
                    isLoading = false,
                    errorText = "",
                    onErrorTextChanged = {},
                    onBack = {},
                    onAddContact = { _, _ -> }
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/images/add_contact_screen_default.png"
        )
    }

    @Test
    fun addContactScreen_layout_checkTouchTargets() {
        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                AddContactContent(
                    initialToxId = "123456789012345678901234567890123456789012345678901234567890123456789012", // valid id
                    showBackButton = true,
                    isLoading = false
                )
            }
        }
        
        // Assert Add button has min touch target of 48dp
        composeTestRule.onNodeWithText("Add").assert(hasMinTouchTargetSize())
        
        // Assert Back button has min touch target of 48dp
        composeTestRule.onNodeWithContentDescription("Back").assert(hasMinTouchTargetSize())
    }

    @Test
    fun addContactScreen_ux_rapidClicks_and_loadingState() {
        var addCount = 0

        composeTestRule.setContent {
            androidx.compose.material3.MaterialTheme {
                AddContactContent(
                    initialToxId = "0000000000000000000000000000000000000000000000000000000000000000000000000000",
                    showBackButton = true,
                    isLoading = false,
                    errorText = "",
                    onErrorTextChanged = {},
                    onBack = {},
                    onAddContact = { _, _ -> addCount++ }
                )
            }
        }
        
        val addButton = composeTestRule.onNodeWithText("Add")
        
        // Rapid clicks (stress test)
        repeat(10) {
            addButton.performClick()
        }

        // State isolation in the ViewModel will drop subsequent clicks, 
        // but here we are testing the UI component directly which fires the callback.
        // If we want to simulate the real app, we should test with AddContactViewModel, 
        // but since this is just the UI, we verify it fires 10 times if isLoading=false.
        assert(addCount == 10)
    }
}
