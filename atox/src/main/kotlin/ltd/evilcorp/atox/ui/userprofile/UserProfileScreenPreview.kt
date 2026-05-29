// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ltd.evilcorp.atox.ui.theme.AToxTheme
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus

@Preview(name = "User Profile Screen Preview", showBackground = true)
@Composable
fun UserProfileScreenPreview() {
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
            selfAvatarBitmap = null,
            selectedImageUri = null,
            onSelectedImageUriChanged = {},
            onLaunchCamera = {},
            onLaunchGallery = {},
            onSetName = {},
            onSetStatusMessage = {},
            onSetStatus = {},
            onLogout = {},
            onAvatarChanged = {},
            onCropAndSaveAvatar = { _, _, _, _, _, _ -> }
        )
    }
}
