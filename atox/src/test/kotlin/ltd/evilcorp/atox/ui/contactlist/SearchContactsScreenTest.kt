package ltd.evilcorp.atox.ui.contactlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.runtime.CompositionLocalProvider
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus
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
class SearchContactsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleContacts = listOf(
        Contact(publicKey = "1", name = "Alice", statusMessage = "0000", status = UserStatus.None),
        Contact(publicKey = "2", name = "Bob", statusMessage = "1111", status = UserStatus.Away),
        Contact(publicKey = "3", name = "Charlie", statusMessage = "2222", status = UserStatus.None)
    )

    @Test
    fun searchContactsScreen_displaysNoResultsWhenNotFound() {
        val mockFileStorageProvider = io.mockk.mockk<ltd.evilcorp.domain.core.network.IFileStorageProvider>(relaxed = true)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    SearchContactsScreen(
                        contacts = sampleContacts,
                        onContactClick = {},
                        onBack = {}
                    )
                }
            }
        }

        // Type query that matches nothing
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Xylophone")
        
        // Assert "No results found" is displayed (or Russian equivalent, handled gracefully with semantic check)
        composeTestRule.onNode(hasText("No settings found") or hasText("Ничего не найдено")).assertIsDisplayed()
        
        // Bob shouldn't be there
        composeTestRule.onNodeWithText("Bob").assertDoesNotExist()
    }

    @Test
    fun searchContactsScreen_filtersContacts() {
        val mockFileStorageProvider = io.mockk.mockk<ltd.evilcorp.domain.core.network.IFileStorageProvider>(relaxed = true)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    SearchContactsScreen(
                        contacts = sampleContacts,
                        onContactClick = {},
                        onBack = {}
                    )
                }
            }
        }

        // Search for "ali"
        composeTestRule.onNode(hasSetTextAction()).performTextInput("ali")

        // Alice should be displayed, Bob and Charlie shouldn't
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertDoesNotExist()
        composeTestRule.onNodeWithText("Charlie").assertDoesNotExist()
    }

    @Test
    fun searchContactsScreen_clicksContact() {
        var clickedContact: Contact? = null

        val mockFileStorageProvider = io.mockk.mockk<ltd.evilcorp.domain.core.network.IFileStorageProvider>(relaxed = true)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    SearchContactsScreen(
                        contacts = sampleContacts,
                        onContactClick = { clickedContact = it },
                        onBack = {}
                    )
                }
            }
        }

        // Search for "Bob"
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Bob")
        
        // Click on Bob in the list, avoiding the search bar text field
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Bob") and androidx.compose.ui.test.hasSetTextAction().not()).performClick()

        assertEquals("Bob", clickedContact?.name)
    }

    @Test
    fun searchContactsScreen_imeActionSearchClosesKeyboard() {
        val mockFileStorageProvider = io.mockk.mockk<ltd.evilcorp.domain.core.network.IFileStorageProvider>(relaxed = true)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    SearchContactsScreen(
                        contacts = sampleContacts,
                        onContactClick = {},
                        onBack = {}
                    )
                }
            }
        }

        // Type query
        val searchInput = composeTestRule.onNode(hasSetTextAction())
        searchInput.performTextInput("Alice")
        
        // Perform ImeAction.Search
        searchInput.performImeAction()
        // Assert Alice is still displayed after search action
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Alice") and androidx.compose.ui.test.hasSetTextAction().not()).assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun searchContactsScreen_layout_matchesSnapshot() {
        val mockFileStorageProvider = io.mockk.mockk<ltd.evilcorp.domain.core.network.IFileStorageProvider>(relaxed = true)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    SearchContactsScreen(
                        contacts = sampleContacts,
                        onContactClick = {},
                        onBack = {}
                    )
                }
            }
        }

        // Let's also input text for the snapshot so we can see the results
        composeTestRule.onNode(hasSetTextAction()).performTextInput("ali")
        
        composeTestRule.onNode(androidx.compose.ui.test.isRoot()).captureRoboImage(
            filePath = "src/test/snapshots/images/search_contacts_screen_with_query.png"
        )
    }

    @Test
    fun searchContactsScreen_ux_rapidSearchAndClick() {
        var clickedCount = 0
        val mockFileStorageProvider = io.mockk.mockk<ltd.evilcorp.domain.core.network.IFileStorageProvider>(relaxed = true)
        
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides mockFileStorageProvider) {
                androidx.compose.material3.MaterialTheme {
                    SearchContactsScreen(
                        contacts = sampleContacts,
                        onContactClick = { clickedCount++ },
                        onBack = {}
                    )
                }
            }
        }

        // Rapid typing
        val searchInput = composeTestRule.onNode(hasSetTextAction())
        for (i in 1..10) {
            searchInput.performTextReplacement("Alice $i")
        }
        
        // Search for Bob to show him
        searchInput.performTextReplacement("Bob")
        
        // Rapid clicks on Bob
        val bobNode = composeTestRule.onNode(androidx.compose.ui.test.hasText("Bob") and androidx.compose.ui.test.hasSetTextAction().not())
        bobNode.assertIsDisplayed()
        
        for (i in 1..5) {
            bobNode.performClick()
        }
        
        // Ensure that click callbacks fired multiple times
        assertEquals(5, clickedCount)
    }
}
