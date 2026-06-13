package ltd.evilcorp.atox.ui.groupchat

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.MutableStateFlow

@RunWith(AndroidJUnit4::class)
class CreateGroupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCreateGroupScreen_showsInputsAndButton() {
        val isCreatingState = MutableStateFlow(false)

        composeTestRule.setContent {
            CreateGroupScreen(
                onBack = {},
                isCreatingState = isCreatingState,
                onCreateGroup = { _, _, _ -> true }
            )
        }

        // Test labels and buttons are visible
        composeTestRule.onNodeWithText("Group Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Public").assertIsDisplayed()
        composeTestRule.onNodeWithText("Private").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Create Group")[0].assertIsDisplayed()
    }
}
