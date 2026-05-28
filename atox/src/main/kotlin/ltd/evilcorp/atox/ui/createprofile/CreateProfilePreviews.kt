package ltd.evilcorp.atox.ui.createprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ltd.evilcorp.atox.ui.theme.AToxTheme

@Preview(name = "Create Profile Screen Preview", showSystemUi = true)
@Composable
fun CreateProfileScreenPreview() {
    AToxTheme {
        CreateProfileContent(
            isLoading = false,
            errorText = "",
            onErrorChanged = {},
            onCreateProfile = {},
            onRestoreBackup = { _, _ -> }
        )
    }
}
