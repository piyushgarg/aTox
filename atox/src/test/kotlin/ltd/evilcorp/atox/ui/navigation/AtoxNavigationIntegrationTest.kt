package ltd.evilcorp.atox.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.test.onNodeWithText

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AtoxNavigationIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainNavigation_transitionsBetweenTabs() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        
        composeTestRule.setContent {
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            NavHost(navController = navController, startDestination = "chats") {
                composable("chats") { androidx.compose.material3.Text("Chats Screen") }
                composable("add_contact") { androidx.compose.material3.Text("Add Contact Screen") }
                composable("profile") { androidx.compose.material3.Text("Profile Screen") }
                composable("settings") { androidx.compose.material3.Text("Settings Screen") }
            }
        }
        
        // Assert start
        composeTestRule.onNodeWithText("Chats Screen").assertExists()

        // Navigate to Add Contact
        composeTestRule.runOnUiThread {
            navController.navigate("add_contact")
        }
        composeTestRule.onNodeWithText("Add Contact Screen").assertExists()
    }
}
