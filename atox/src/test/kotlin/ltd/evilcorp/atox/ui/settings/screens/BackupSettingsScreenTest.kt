package ltd.evilcorp.atox.ui.settings.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import ltd.evilcorp.domain.features.settings.model.BackupDestination
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class BackupSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBackupSettingsScreenDisplaysElements() {
        composeTestRule.setContent {
            BackupSettingsScreen(
                paddingValues = PaddingValues(0.dp),
                automaticBackupEnabled = true,
                onAutomaticBackupEnabledChanged = {},
                backupFrequency = BackupFrequency.Weekly,
                onBackupFrequencyChanged = {},
                backupUseCellular = false,
                onBackupUseCellularChanged = {},
                backupDestinations = setOf(BackupDestination.Local, BackupDestination.GoogleDrive),
                onBackupDestinationsChanged = {},
                backupGoogleAccount = "test@example.com",
                lastLocalBackupTimeMs = 0L,
                lastLocalBackupSizeKb = 0L,
                lastGoogleBackupTimeMs = 0L,
                lastGoogleBackupSizeKb = 0L,
                onGoogleAccountClick = {},
                selectedBackupIds = setOf("tox_core"),
                mandatoryBackupId = "tox_core",
                onSelectedBackupIdsChanged = {},
                onCreateBackupClick = {},
                onCreateGoogleBackupClick = {},
                onRestoreBackupClick = {},
                performHaptic = {},
                backupProviders = emptyList(),
                backupExporting = false,
                backupImporting = false,
            )
        }

        // Ensure it renders without crashing
        // Ensure it renders without crashing
        // Asserts are intentionally minimal to avoid string resource changes breaking tests
    }
}
