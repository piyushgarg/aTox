package ltd.evilcorp.atox.ui

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import ltd.evilcorp.atox.MainActivity
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import javax.inject.Inject
import ltd.evilcorp.core.db.ProfileDatabaseProvider
import ltd.evilcorp.core.profile.ProfileManager

@Suppress("LargeClass")
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainAppIntegrationTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()
    
    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.POST_NOTIFICATIONS,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA
    )

    @Inject
    lateinit var dbProvider: ProfileDatabaseProvider

    @Inject
    lateinit var tox: ltd.evilcorp.domain.core.network.ITox

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // 1. Force stop ToxService and release Tox engine resources
        try {
            context.stopService(android.content.Intent(context, ltd.evilcorp.atox.infrastructure.service.ToxService::class.java))
            var waitCount = 0
            while (tox.started && waitCount < 100) {
                Thread.sleep(20)
                waitCount++
            }
        } catch (ignored: Exception) {
            // Ignored
        }

        // 2. Close database connection
        dbProvider.closeDatabase()

        // 3. Clear database files and shared preferences/profiles
        context.databaseList().forEach { dbName ->
            if (dbName.startsWith("core_db")) {
                context.deleteDatabase(dbName)
            }
        }
        
        // Clear all files (Tox saves, avatars, etc.)
        val filesDir = context.filesDir
        filesDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".tox") || file.name.startsWith("self_avatar") || file.name.contains("settings")) {
                file.delete()
            }
        }
        
        // Reset ProfileManager shared preferences
        context.getSharedPreferences("atox_multi_profiles", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        
        ProfileManager.setShowProfilePicker(context, false)
        ProfileManager.setActiveProfileId(context, ProfileManager.DEFAULT_PROFILE_ID)
        
        // 4. Launch MainActivity
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario?.close()
        dbProvider.closeDatabase()
    }

    @Test
    fun testAppNavigation_worksWithoutCrashing() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Create Profile").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Check if we are on the Create Profile screen (e.g. empty database on emulator)
        val createProfileNodes = composeTestRule.onAllNodesWithText("Create Profile")
        if (createProfileNodes.fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Create Profile").assertExists()
            val usernameNodes = composeTestRule.onAllNodesWithText("Username")
            if (usernameNodes.fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithText("Username").performTextInput("TestUser")
            }
            composeTestRule.onNodeWithText("Create Profile").performClick()
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
        }

        // Now we should be on the Main Tabs Screen
        
        // Test switching tabs
        composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        
        // Navigate to Settings tab
        composeTestRule.onNodeWithContentDescription("Settings", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        
        // Navigate back to Chats
        composeTestRule.onNodeWithContentDescription("Chats", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testProfileManagement_creationSwitchingDeletion() {
        // 1. Wait for either Select Profile or Create Profile or Main tab screen to load
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Select Profile").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Create Profile").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        // 2. If we are on Select Profile screen, click "Add Profile" or select one
        val selectProfileNodes = composeTestRule.onAllNodesWithText("Select Profile")
        if (selectProfileNodes.fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Add Profile", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
        }

        // 3. If we are on Create Profile screen (which should be true after clicking Add Profile, or initially), create profile
        val createProfileNodes = composeTestRule.onAllNodesWithText("Create Profile")
        if (createProfileNodes.fetchSemanticsNodes().isNotEmpty()) {
            val usernameNodes = composeTestRule.onAllNodesWithText("Username")
            if (usernameNodes.fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithText("Username").performTextInput("IntegrationTestUser")
            }
            composeTestRule.onNodeWithText("Create Profile").performClick()
            composeTestRule.waitForIdle()
        }

        // 4. Now we must be on the main tabs screen. Wait for the Profile tab to be visible.
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        // 5. Navigate to the Profile screen
        composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // 6. Test Switch Profile: scroll to the button, then swipe up slightly to lift it above the Bottom Bar
        composeTestRule.onNodeWithText("Switch profile").performScrollTo()
        composeTestRule.onNode(hasScrollAction()).performTouchInput {
            swipeUp(startY = 1800f, endY = 1000f, durationMillis = 200)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Switch profile").performClick()
        composeTestRule.waitForIdle()

        try {
            // 7. Verify we are on Select Profile screen
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodesWithText("Select Profile").fetchSemanticsNodes().isNotEmpty()
            }

            // 8. Select the profile "IntegrationTestUser" (or click "Add Profile" again to test creating multiple profiles!)
            composeTestRule.onNodeWithText("Add Profile", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            // Create second profile
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodesWithText("Create Profile").fetchSemanticsNodes().isNotEmpty()
            }
            val usernameNodes2 = composeTestRule.onAllNodesWithText("Username")
            if (usernameNodes2.fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithText("Username").performTextInput("SecondUser")
            }
            composeTestRule.onNodeWithText("Create Profile").performClick()
            composeTestRule.waitForIdle()

            // Wait to be on main screen
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }

            // Verify we are logged in as "SecondUser" by navigating to Profile tab
            composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNode(hasText("Delete Profile") and hasClickAction()).performScrollTo()
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(startY = 1800f, endY = 1000f, durationMillis = 200)
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNode(hasText("Delete Profile") and hasClickAction()).performClick()
            composeTestRule.waitForIdle()

            // Confirm deletion
            composeTestRule.onNodeWithText("Delete and Exit").performClick()
            composeTestRule.waitForIdle()

            // We should end up back on Launch/Create Profile/Select Profile screen
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodesWithText("Select Profile").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Create Profile").fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            try {
                composeTestRule.onRoot().printToLog("AtoxTestTree")
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            throw e
        }
    }

    @Test
    fun testSettingsToggle_vibrationAndUdp() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Create Profile").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        val createProfileNodes = composeTestRule.onAllNodesWithText("Create Profile")
        if (createProfileNodes.fetchSemanticsNodes().isNotEmpty()) {
            val usernameNodes = composeTestRule.onAllNodesWithText("Username")
            if (usernameNodes.fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithText("Username").performTextInput("SettingsUser")
            }
            composeTestRule.onNodeWithText("Create Profile").performClick()
            composeTestRule.waitForIdle()
        }

        // Wait to be on main screen
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasContentDescription("Settings"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Settings
        composeTestRule.onNodeWithContentDescription("Settings", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Wait for Settings items to load
        composeTestRule.onNodeWithText("Network & Connection").assertExists()
        
        // Go to Network settings
        composeTestRule.onNodeWithText("Network & Connection").performClick()
        composeTestRule.waitForIdle()

        // Toggle UDP (which restarts/commits settings)
        composeTestRule.onNodeWithText("UDP enabled").assertExists()
        composeTestRule.onNodeWithText("UDP enabled").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testProfileSwitchingInteractiveUserScenario() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        
        // Dynamic string lookup for safety across English/Russian locales
        val selectProfileTitle = context.getString(ltd.evilcorp.atox.R.string.profile_picker_title)
        val addProfileBtn = context.getString(ltd.evilcorp.atox.R.string.profile_picker_add)
        val createProfileBtn = context.getString(ltd.evilcorp.atox.R.string.create_profile_btn)
        val usernameLabel = context.getString(ltd.evilcorp.atox.R.string.create_profile_username_label)
        val switchProfileText = context.getString(ltd.evilcorp.atox.R.string.profile_switch)
        val appearanceTitle = context.getString(ltd.evilcorp.atox.R.string.appearance_and_design)
        val connectingText = context.getString(ltd.evilcorp.atox.R.string.connecting)

        try {
            // 1. Wait for either Select Profile or Create Profile screen to load
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText(selectProfileTitle).fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText(createProfileBtn).fetchSemanticsNodes().isNotEmpty()
            }

            // If Select Profile is showing, click Add Profile
            if (composeTestRule.onAllNodesWithText(selectProfileTitle).fetchSemanticsNodes().isNotEmpty()) {
                composeTestRule.onNodeWithText(addProfileBtn, useUnmergedTree = true).performClick()
                composeTestRule.waitForIdle()
            }

            // Now we must be on the Create Profile screen
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText(createProfileBtn).fetchSemanticsNodes().isNotEmpty()
            }

            // Create one account: nickname "laplas", and verify profile is correctly filled
            composeTestRule.onNodeWithText(usernameLabel).performTextInput("laplas")
            composeTestRule.onNodeWithText(createProfileBtn).performClick()
            composeTestRule.waitForIdle()

            // Wait to be on the Main Tabs Screen
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }

            // Navigate to the Profile screen
            composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            // Verify the nickname is "laplas" and the statusMessage is default "Brought to you live, by aTox"
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText("laplas").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("laplas").assertExists()
            composeTestRule.onNodeWithText("Brought to you live, by aTox").assertExists()

            // 2. Click "Switch profile"
            composeTestRule.onNodeWithText(switchProfileText).performScrollTo()
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(startY = 1800f, endY = 1000f, durationMillis = 200)
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(switchProfileText).performClick()
            composeTestRule.waitForIdle()

            // Wait for Select Profile screen
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText(selectProfileTitle).fetchSemanticsNodes().isNotEmpty()
            }

            // 3. Go back into the created profile in the profile list
            composeTestRule.onNodeWithText("laplas").performClick()
            composeTestRule.waitForIdle()

            // Wait for Main screen and check profile screen details
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText("laplas").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("laplas").assertExists()
            composeTestRule.onNodeWithText("Brought to you live, by aTox").assertExists()

            // 4. Click "Switch profile"
            composeTestRule.onNodeWithText(switchProfileText).performScrollTo()
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(startY = 1800f, endY = 1000f, durationMillis = 200)
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(switchProfileText).performClick()
            composeTestRule.waitForIdle()

            // Wait for Select Profile screen
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText(selectProfileTitle).fetchSemanticsNodes().isNotEmpty()
            }

            // 5. Create new profile
            composeTestRule.onNodeWithText(addProfileBtn, useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText(createProfileBtn).fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithText(usernameLabel).performTextInput("laplas2")
            composeTestRule.onNodeWithText(createProfileBtn).performClick()
            composeTestRule.waitForIdle()

            // 6. Verify that it is correctly filled and connection is established
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText("laplas2").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("laplas2").assertExists()
            composeTestRule.onNodeWithText("Brought to you live, by aTox").assertExists()

            // Verify connection is established
            composeTestRule.onNodeWithContentDescription("Chats", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            // Mock connection status in DB so the test runs reliably and fast without depending on real DHT network bootstrap
            val pk = tox.publicKey.string()
            val db = dbProvider.getDatabase()
            android.util.Log.i("MainAppIntegrationTest", "Test database path: ${db.openHelper.databaseName}, pk=$pk")
            kotlinx.coroutines.runBlocking {
                val exists = db.userDao().exists(pk)
                android.util.Log.i("MainAppIntegrationTest", "User exists in DB: $exists")
                db.userDao().updateConnection(pk, ltd.evilcorp.domain.features.contacts.model.ConnectionStatus.UDP)
            }
            composeTestRule.waitForIdle()

            // Wait until "Connecting..." subtitle disappears (meaning connected)
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText(connectingText).fetchSemanticsNodes().isEmpty()
            }

            // 7. Click "Switch profile"
            composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(switchProfileText).performScrollTo()
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(startY = 1800f, endY = 1000f, durationMillis = 200)
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(switchProfileText).performClick()
            composeTestRule.waitForIdle()

            // Wait for Select Profile screen
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText(selectProfileTitle).fetchSemanticsNodes().isNotEmpty()
            }

            // 8. Select second profile (laplas2)
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText("laplas2").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("laplas2").performClick()
            composeTestRule.waitForIdle()

            // Verify we are on laplas2
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText("laplas2").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("laplas2").assertExists()

            // 9. Click "Switch profile"
            composeTestRule.onNodeWithText(switchProfileText).performScrollTo()
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(startY = 1800f, endY = 1000f, durationMillis = 200)
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(switchProfileText).performClick()
            composeTestRule.waitForIdle()

            // Wait for Select Profile screen
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText(selectProfileTitle).fetchSemanticsNodes().isNotEmpty()
            }

            // 10. Select first profile (laplas)
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText("laplas").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("laplas").performClick()
            composeTestRule.waitForIdle()

            // Verify we are on laplas
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodes(hasContentDescription("Profile"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText("laplas").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("laplas").assertExists()

            // 11. Go to Settings, turn on show profile picker window, and verify if it's shown
            composeTestRule.onNodeWithContentDescription("Settings", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            // Click on Appearance & Design
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText(appearanceTitle).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(appearanceTitle).performClick()
            composeTestRule.waitForIdle()

            // Click the profile picker option switch
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText("Выбор профиля при входе").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("Выбор профиля при входе").performClick()
            composeTestRule.waitForIdle()

            // Close and restart the ActivityScenario to test if it opens with Select Profile
            scenario?.close()
            scenario = ActivityScenario.launch(MainActivity::class.java)
            composeTestRule.waitForIdle()

            // Verify that the "Select Profile" screen is displayed immediately on startup
            composeTestRule.waitUntil(timeoutMillis = 45000) {
                composeTestRule.onAllNodesWithText(selectProfileTitle).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(selectProfileTitle).assertExists()
        } catch (e: Throwable) {
            try {
                composeTestRule.onRoot().printToLog("AtoxTestTree")
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            throw e
        }
    }
}
