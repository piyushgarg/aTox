package ltd.evilcorp.atox.ui.navigation.graphs

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.navigation.AppRoutes
import ltd.evilcorp.atox.ui.navigation.LocalTabPadding
import ltd.evilcorp.atox.ui.userprofile.UserProfileScreen
import ltd.evilcorp.atox.ui.userprofile.UserProfileViewModel
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar

private const val MAX_AVATAR_BYTES = 64 * 1024

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.profileTabRoute(
    navController: NavHostController,
    settings: Settings
) {
    composable<AppRoutes.Profile> {
        val context = LocalContext.current
        val profileViewModel: UserProfileViewModel = hiltViewModel()
        val user by profileViewModel.user.collectAsStateWithLifecycle()
        val connectionStatus = user?.connectionStatus ?: ConnectionStatus.None

        val avatarFile by profileViewModel.avatarFile.collectAsStateWithLifecycle()
        val cropState by profileViewModel.cropState.collectAsStateWithLifecycle()
        val storedSettings by settings.state.collectAsStateWithLifecycle()
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val performHaptic = {
            if (storedSettings.hapticEnabled) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            }
        }

        var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
            if (uri != null) {
                selectedImageUri = uri
            }
        }

        val tempFile = remember { java.io.File(context.cacheDir, "avatar_capture.jpg") }
        val tempFileUri = remember(tempFile) {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        }

        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                selectedImageUri = tempFileUri
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = context.getString(R.string.profile),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (connectionStatus == ConnectionStatus.None) {
                                Text(
                                    text = context.getString(R.string.connecting),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                UserProfileScreen(
                    user = user,
                    toxId = profileViewModel.toxId.string(),
                    avatarFile = avatarFile,
                    cropState = cropState,
                    selectedImageUri = selectedImageUri,
                    onSelectedImageUriChanged = { selectedImageUri = it },
                    onLaunchCamera = {
                        try {
                            cameraLauncher.launch(tempFileUri)
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, "Camera application not found", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onLaunchGallery = {
                        galleryLauncher.launch("image/*")
                    },
                    showBackButton = false,
                    bottomPadding = LocalTabPadding.current.calculateBottomPadding(),
                    onSetName = profileViewModel::setName,
                    onSetStatusMessage = profileViewModel::setStatusMessage,
                    onSetStatus = profileViewModel::setStatus,
                    performHaptic = performHaptic,
                    onLogout = {
                        profileViewModel.deleteProfileAndData()
                        navController.navigate(AppRoutes.Launch) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onAvatarChanged = profileViewModel::broadcastAvatar,
                    onResetCropState = profileViewModel::resetCropState,
                    onCropAndSaveAvatar = { originalBitmap, scale, offsetX, offsetY, rotation, viewportWidth ->
                        val cropped = ltd.evilcorp.atox.ui.userprofile.AvatarCropUtils.cropAvatar(
                            bitmap = originalBitmap,
                            scale = scale,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            rotation = rotation,
                            viewportWidth = viewportWidth
                        )
                        val bytes = ltd.evilcorp.atox.ui.userprofile.AvatarCropUtils.compressToJpeg(cropped, MAX_AVATAR_BYTES)
                        if (bytes != null) {
                            profileViewModel.saveAvatar(bytes)
                        }
                    }
                )
            }
        }
    }
}
