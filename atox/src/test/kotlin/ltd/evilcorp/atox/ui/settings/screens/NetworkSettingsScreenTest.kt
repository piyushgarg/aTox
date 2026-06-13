package ltd.evilcorp.atox.ui.settings.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import io.mockk.mockk
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.ProxyType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class NetworkSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNetworkSettingsScreenDisplaysElements() {
        val mockFocusManager = mockk<FocusManager>(relaxed = true)

        composeTestRule.setContent {
            NetworkSettingsScreen(
                paddingValues = PaddingValues(0.dp),
                udpEnabled = true,
                runAtStartup = false,
                bootstrapNodeSource = BootstrapNodeSource.BuiltIn,
                disableScreenshots = false,
                confirmQuitting = true,
                confirmCalling = true,
                proxyType = ProxyType.None,
                proxyAddress = "",
                proxyPortInput = "",
                focusManager = mockFocusManager,
                performHaptic = {},
                onUdpEnabledChanged = {},
                onRunAtStartupChanged = {},
                onBootstrapNodesClick = {},
                onDisableScreenshotsChanged = {},
                onConfirmQuittingChanged = {},
                onConfirmCallingChanged = {},
                onProxyTypeClick = {},
                onProxyAddressChanged = {},
                onProxyPortInputChanged = {}
            )
        }

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.settings_network_group)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.pref_udp_enabled)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.settings_proxy_group)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.settings_proxy_type)).performScrollTo().assertIsDisplayed()
    }
}
