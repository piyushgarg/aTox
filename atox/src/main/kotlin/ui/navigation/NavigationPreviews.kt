package ltd.evilcorp.atox.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ltd.evilcorp.atox.ui.theme.AToxTheme

@Preview(name = "Launch Screen Loading", showBackground = true)
@Composable
private fun LaunchScreenPreview() {
    AToxTheme {
        LaunchScreenContent()
    }
}

@Preview(name = "Unlock Screen Normal", showBackground = true)
@Composable
private fun UnlockScreenPreview() {
    AToxTheme {
        UnlockScreenContent(
            isError = false,
            isLoading = false,
            onSubmitUnlock = {},
            onQuit = {},
            onClearError = {}
        )
    }
}

@Preview(name = "Unlock Screen Error", showBackground = true)
@Composable
private fun UnlockScreenErrorPreview() {
    AToxTheme {
        UnlockScreenContent(
            isError = true,
            isLoading = false,
            onSubmitUnlock = {},
            onQuit = {},
            onClearError = {}
        )
    }
}

@Preview(name = "Unlock Screen Loading State", showBackground = true)
@Composable
private fun UnlockScreenLoadingPreview() {
    AToxTheme {
        UnlockScreenContent(
            isError = false,
            isLoading = true,
            onSubmitUnlock = {},
            onQuit = {},
            onClearError = {}
        )
    }
}

@Preview(name = "Main Tabs Screen - Chats Tab", showBackground = true)
@Composable
private fun MainTabsChatsPreview() {
    AToxTheme {
        MainTabsScreen(
            currentRoute = AppRoutes.Chats,
            attentionCount = 5,
            hapticEnabled = true,
            onTabSelected = {},
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Chats Content Placeholder")
            }
        }
    }
}

@Preview(name = "Main Tabs Screen - Settings Tab", showBackground = true)
@Composable
private fun MainTabsSettingsPreview() {
    AToxTheme {
        MainTabsScreen(
            currentRoute = AppRoutes.Settings,
            attentionCount = 0,
            hapticEnabled = true,
            onTabSelected = {},
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Settings Content Placeholder")
            }
        }
    }
}
