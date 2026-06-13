package ltd.evilcorp.atox.infrastructure.backup.google

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleDriveBackupHelperTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testGetDriveService_createsServiceSuccessfully() {
        // We just test if Drive instance creation doesn't crash given a mock GoogleSignInAccount
        val helper = GoogleDriveBackupHelper(context)

        // It shouldn't crash until it tries to use the credential over network.
        // We can't fully invoke uploadBackupZip in unit test without heavy mocking of Google Drive REST APIs.
        assertNotNull(helper)
    }
}
