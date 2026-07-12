package ltd.evilcorp.atox.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.settings.common.SettingsGroup
import ltd.evilcorp.atox.ui.settings.common.SettingsSwitchRow
import ltd.evilcorp.atox.ui.settings.common.SettingsClickableRow

private val ContentPaddingTop = 16.dp
private val ContentPaddingBottomDefault = 32.dp
private val HorizontalMargin = 16.dp
private val SpacingSpacedBy = 16.dp
private const val DIVIDER_ALPHA = 0.08f

@Composable
fun PrivacySettingsScreen(
    paddingValues: PaddingValues,
    disableScreenshots: Boolean,
    confirmCalling: Boolean,
    isBiometricHardwareAvailable: Boolean,
    biometricEnabled: Boolean,
    hasPassword: Boolean,
    performHaptic: () -> Unit,
    onDisableScreenshotsChanged: (Boolean) -> Unit,
    onConfirmCallingChanged: (Boolean) -> Unit,
    onBiometricEnabledChanged: (Boolean) -> Unit,
    onChangePasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = ltd.evilcorp.atox.ui.navigation.LocalTabPadding.current.calculateBottomPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = HorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(SpacingSpacedBy),
        contentPadding = PaddingValues(
            top = ContentPaddingTop,
            bottom = ContentPaddingBottomDefault + bottomPadding
        )
    ) {
        item {
            SettingsGroup(title = stringResource(R.string.settings_privacy_group)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_block_screenshots),
                    subtitle = stringResource(R.string.pref_block_screenshots_description),
                    checked = disableScreenshots
                ) { checked ->
                    performHaptic()
                    onDisableScreenshotsChanged(checked)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = DIVIDER_ALPHA))
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_confirm_calling),
                    subtitle = stringResource(R.string.call_confirm),
                    checked = confirmCalling
                ) { checked ->
                    performHaptic()
                    onConfirmCallingChanged(checked)
                }
                if (isBiometricHardwareAvailable && hasPassword) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = DIVIDER_ALPHA))
                    SettingsSwitchRow(
                        title = stringResource(R.string.pref_biometric_login),
                        subtitle = stringResource(R.string.pref_biometric_login_description),
                        checked = biometricEnabled
                    ) { checked ->
                        performHaptic()
                        onBiometricEnabledChanged(checked)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = DIVIDER_ALPHA))
                SettingsClickableRow(
                    title = stringResource(
                        if (hasPassword) R.string.pref_heading_change_password else R.string.pref_heading_set_password
                    ),
                    subtitle = stringResource(
                        if (hasPassword) R.string.pref_change_password_description else R.string.pref_set_password_description
                    ),
                    onClick = {
                        performHaptic()
                        onChangePasswordClick()
                    }
                )
            }
        }
    }
}
