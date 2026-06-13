package ltd.evilcorp.atox.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun authNavigation_navigatingToChats_popsLaunchScreen() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.navigatorProvider.addNavigator(ComposeNavigator())

        composeTestRule.setContent {
            NavHost(navController = navController, startDestination = "launch") {
                composable("launch") { androidx.compose.material3.Text("Launch Screen") }
                composable("chats") { androidx.compose.material3.Text("Chats Screen") }
            }
        }
        
        composeTestRule.onNodeWithText("Launch Screen").assertExists()

        // Simulate navigating to chats and popping launch screen
        composeTestRule.runOnUiThread {
            navController.navigate("chats") {
                popUpTo("launch") { inclusive = true }
            }
        }
        
        composeTestRule.onNodeWithText("Chats Screen").assertExists()
        
        // Assert that the backstack has been cleared of the launch screen
        val backStackSize = navController.currentBackStack.value.size
        // 1 for NavHost root, 1 for chats destination
        assertEquals(2, backStackSize) 
    }
}
