package ltd.evilcorp.atox.ui.createprofile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.AtoxLoadingButton
import ltd.evilcorp.atox.ui.userprofile.components.AvatarSourceDialog
import ltd.evilcorp.atox.ui.userprofile.components.AvatarEditDialog
import ltd.evilcorp.atox.ui.userprofile.AvatarCropUtils

@Composable
fun CreateProfileScreenNew(
    viewModel: CreateProfileViewModel,
    onSuccess: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var errorText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state) {
        when (state) {
            is CreateProfileUiState.Success -> {
                keyboardController?.hide()
                onSuccess()
            }
            is CreateProfileUiState.Error -> {
                keyboardController?.hide()
                val errorResId = (state as CreateProfileUiState.Error).errorResId
                errorText = context.getString(errorResId)
            }
            else -> {}
        }
    }

    CreateProfileContentNew(
        isLoading = state is CreateProfileUiState.Loading,
        errorText = errorText,
        onErrorChanged = { errorText = it },
        onCreateProfile = { name, bio, avatarBytes ->
            keyboardController?.hide()
            viewModel.createProfile(name, bio, avatarBytes)
        },
        onRestoreBackup = onRestoreBackup
    )
}

@Composable
fun CreateProfileContentNew(
    isLoading: Boolean,
    errorText: String,
    onErrorChanged: (String) -> Unit = {},
    onCreateProfile: (String, String, ByteArray?) -> Unit = { _, _, _ -> },
    onRestoreBackup: () -> Unit = {},
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showAvatarSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
        }
    }

    val tempCameraUri = remember {
        val tempFile = java.io.File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
        tempFile.createNewFile()
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedImageUri = tempCameraUri
        }
    }

    if (showAvatarSourceDialog) {
        AvatarSourceDialog(
            onDismissRequest = { showAvatarSourceDialog = false },
            onCameraClick = {
                showAvatarSourceDialog = false
                cameraLauncher.launch(tempCameraUri)
            },
            onGalleryClick = {
                showAvatarSourceDialog = false
                galleryLauncher.launch("image/*")
            }
        )
    }

    // Avatar crop dialog
    if (selectedImageUri != null) {
        AvatarEditDialog(
            imageUri = selectedImageUri!!,
            onDismiss = { selectedImageUri = null },
            onConfirm = { originalBitmap, scale, offsetX, offsetY, rotation, viewportWidth ->
                val cropped = AvatarCropUtils.cropAvatar(
                    bitmap = originalBitmap,
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    rotation = rotation,
                    viewportWidth = viewportWidth
                )
                val bytes = AvatarCropUtils.compressToJpeg(cropped, 64 * 1024)
                if (bytes != null) {
                    avatarBytes = bytes
                    selectedImageUri = null
                } else {
                    android.util.Log.e("CreateProfile", "Failed to compress avatar")
                    selectedImageUri = null
                }
            }
        )
    }

    val submitProfile = {
        if (nameInput.trim().isEmpty()) {
            onErrorChanged(context.getString(R.string.create_profile_error_empty))
        } else {
            onCreateProfile(nameInput.trim(), bioInput.trim(), avatarBytes)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.ime)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_profile_welcome),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.create_profile_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = !isLoading) { showAvatarSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBytes != null) {
                            val bitmap = remember(avatarBytes) {
                                try {
                                    val bmp = android.graphics.BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes!!.size)
                                    bmp?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = stringResource(R.string.profile_avatar_change),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Fallback icon with dark overlay
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = stringResource(R.string.profile_avatar_change),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_camera),
                                        contentDescription = stringResource(R.string.profile_avatar_change),
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        } else {
                            // No avatar selected: show camera icon with dark overlay
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = stringResource(R.string.profile_avatar_change),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                modifier = Modifier.size(64.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_camera),
                                    contentDescription = stringResource(R.string.profile_avatar_change),
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    // Edit button in bottom-right corner when avatar is selected
                    if (avatarBytes != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable(enabled = !isLoading) { showAvatarSourceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.profile_avatar_change),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.profile_avatar_change),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Name
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        if (it.isNotEmpty()) onErrorChanged("")
                    },
                    label = { Text(stringResource(R.string.create_profile_username_label)) },
                    placeholder = { Text(stringResource(R.string.create_profile_username_placeholder)) },
                    isError = errorText.isNotEmpty(),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Bio
                OutlinedTextField(
                    value = bioInput,
                    onValueChange = { bioInput = it },
                    label = { Text(stringResource(R.string.status_message)) },
                    placeholder = { Text(stringResource(R.string.status_message_default)) },
                    maxLines = 3,
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = true,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitProfile() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AtoxLoadingButton(
                    onClick = { submitProfile() },
                    text = stringResource(R.string.create_profile_btn),
                    isLoading = isLoading,
                    enabled = nameInput.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                )

                TextButton(
                    onClick = onRestoreBackup,
                    enabled = !isLoading
                ) {
                    Text(
                        text = stringResource(R.string.backup_restore_from_file),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
