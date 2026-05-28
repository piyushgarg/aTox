package ltd.evilcorp.atox.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.settings.common.SettingsDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenTopBar(
    title: String,
    destination: SettingsDestination,
    onBack: () -> Unit,
    performHaptic: () -> Unit,
    onDestinationChanged: (SettingsDestination) -> Unit
) {
    if (destination != SettingsDestination.Search) {
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                if (destination != SettingsDestination.Root) {
                    IconButton(onClick = {
                        performHaptic()
                        onDestinationChanged(
                            when (destination) {
                                SettingsDestination.Language, SettingsDestination.Theme -> SettingsDestination.Appearance
                                SettingsDestination.Search -> SettingsDestination.Root
                                else -> SettingsDestination.Root
                            }
                        )
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigation_back)
                        )
                    }
                } else {
                    IconButton(onClick = {
                        performHaptic()
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigation_back)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        )
    }
}
