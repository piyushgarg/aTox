package ltd.evilcorp.atox.ui.profile

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.atox.ui.userprofile.UserProfileScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.runtime.CompositionLocalProvider
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider
import ltd.evilcorp.domain.core.network.IFileStorageProvider

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeFileStorageProvider = object : IFileStorageProvider {
        override fun exists(uriString: String): Boolean = false
        override fun lastModified(uriString: String): Long = 0L
        override fun size(uriString: String): Long = 0L
        override fun getAbsolutePath(uriString: String): String? = null
    }

    @Test
    fun testProfileScreen_showsToxId() {
        val testToxId = "1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF12345678"
        val fakeUser = User(
            publicKey = testToxId,
            name = "MyName",
            statusMessage = "MyStatus",
            status = UserStatus.None
        )
        
        composeTestRule.setContent {
            CompositionLocalProvider(LocalFileStorageProvider provides fakeFileStorageProvider) {
                UserProfileScreen(
                    user = fakeUser,
                    toxId = testToxId,
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

        composeTestRule.onNodeWithText(testToxId).assertIsDisplayed()
        composeTestRule.onNodeWithText("MyName").assertIsDisplayed()
        composeTestRule.onNodeWithText("MyStatus").assertIsDisplayed()
    }
}
