package ltd.evilcorp.atox.ui.chat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule as createComposeRuleV2
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.mockk
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider
import ltd.evilcorp.domain.core.network.IFileStorageProvider
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class ForwardSelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRuleV2()

    private val sampleContacts = listOf(
        Contact(publicKey = "1", name = "Alice", statusMessage = "0000", status = UserStatus.None),
        Contact(publicKey = "2", name = "Bob", statusMessage = "1111", status = UserStatus.Away),
        Contact(publicKey = "3", name = "Charlie", statusMessage = "2222", status = UserStatus.None)
    )

    private val mockSettings = mockk<Settings>(relaxed = true)
    private val mockFileStorageProvider = mockk<IFileStorageProvider>(relaxed = true)

    @Test
    fun forwardSelectionScreen_selectsContactAndShowsButton() {
        var selectedContacts: List<Contact>? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ForwardSelectionScreen(
                        contacts = sampleContacts,
                        settings = mockSettings,
                        onBack = {},
                        onContactsSelect = { selectedContacts = it }
                    )
                }
            }
        }

        // Click on Alice
        composeTestRule.onNode(hasText("Alice") and !hasSetTextAction()).performClick()
        
        // "Forward (1)" button should be displayed
        composeTestRule.onNodeWithText("Forward (1)").assertIsDisplayed()

        // Click on Bob
        composeTestRule.onNode(hasText("Bob") and !hasSetTextAction()).performClick()

        // "Forward (2)" button should be displayed
        composeTestRule.onNodeWithText("Forward (2)").assertIsDisplayed()

        // Unselect Alice
        composeTestRule.onNode(hasText("Alice") and !hasSetTextAction()).performClick()

        // "Forward (1)" button should be displayed
        composeTestRule.onNodeWithText("Forward (1)").assertIsDisplayed()

        // Click the forward button
        composeTestRule.onNodeWithText("Forward (1)").performClick()

        // Verify callback
        assertEquals(1, selectedContacts?.size)
        assertEquals("Bob", selectedContacts?.first()?.name)
    }

    @Test
    fun forwardSelectionScreen_filtersContacts() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ForwardSelectionScreen(
                        contacts = sampleContacts,
                        settings = mockSettings,
                        onBack = {},
                        onContactsSelect = {}
                    )
                }
            }
        }

        // Open search
        composeTestRule.onNodeWithContentDescription("Search").performClick()
        composeTestRule.waitForIdle()

        // Search for "ali"
        composeTestRule.onNode(hasSetTextAction()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("ali")
        composeTestRule.waitForIdle()

        // Alice should be displayed, Bob and Charlie shouldn't
        // Use [1] because the background list has an occluded Alice at index 0
        composeTestRule.onAllNodes(hasText("Alice") and !hasSetTextAction())[1].assertIsDisplayed()
        composeTestRule.onNode(hasText("Bob") and !hasSetTextAction()).assertDoesNotExist()
        composeTestRule.onNode(hasText("Charlie") and !hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun forwardSelectionScreen_searchOverlayTransitionAnimations() {
        composeTestRule.mainClock.autoAdvance = false
        
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ForwardSelectionScreen(
                        contacts = sampleContacts,
                        settings = mockSettings,
                        onBack = {},
                        onContactsSelect = {}
                    )
                }
            }
        }
        
        // Open search
        composeTestRule.onNodeWithContentDescription("Search").performClick()
        
        // Advance clock halfway (e.g. 150ms) to check intermediate state
        composeTestRule.mainClock.advanceTimeBy(150L)
        
        // Let it finish
        composeTestRule.mainClock.advanceTimeBy(1000L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        
        // Should be fully visible
        composeTestRule.onNode(hasSetTextAction()).assertIsDisplayed()
        
        // Close search
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(150L)
        
        // Let it finish
        composeTestRule.mainClock.advanceTimeBy(1000L)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        
        // Search should be gone
        composeTestRule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun forwardSelectionScreen_layout_matchesSnapshot() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    ForwardSelectionScreen(
                        contacts = sampleContacts,
                        settings = mockSettings,
                        onBack = {},
                        onContactsSelect = {}
                    )
                }
            }
        }

        // Select one contact to show the Forward button
        composeTestRule.onNode(hasText("Alice") and !hasSetTextAction()).performClick()

        composeTestRule.onNode(isRoot()).captureRoboImage(
            filePath = "src/test/snapshots/images/forward_selection_screen.png"
        )
    }
}
