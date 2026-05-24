package ltd.evilcorp.atox.ui.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import ltd.evilcorp.atox.ui.theme.AToxTheme
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.User
import ltd.evilcorp.core.model.UserStatus

@Preview(name = "User Profile Screen Preview", showSystemUi = true)
@Composable
private fun UserProfileScreenPreview() {
    val mockUser = User(
        publicKey = "1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF",
        name = "Sergey Ivanov",
        statusMessage = "Coding aTox with pure architectures",
        status = UserStatus.None,
        connectionStatus = ConnectionStatus.TCP
    )

    AToxTheme {
        UserProfileScreen(
            user = mockUser,
            toxId = "1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890AB",
            avatar = null,
            onSetName = {},
            onSetStatusMessage = {},
            onSetStatus = {},
            onLogout = {},
            onAvatarChanged = {},
            onCropAndSaveAvatar = { _, _, _, _, _, _ -> true }
        )
    }
}
