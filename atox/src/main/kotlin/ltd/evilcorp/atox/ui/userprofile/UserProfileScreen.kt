// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.userprofile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.QrCodeView
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusBusy
import ltd.evilcorp.atox.ui.userprofile.components.AvatarEditDialog
import ltd.evilcorp.atox.ui.common.AtoxConfirmDialog
import ltd.evilcorp.atox.ui.userprofile.components.QrCodeDialog
import ltd.evilcorp.atox.ui.userprofile.components.AvatarProcessingDialog
import ltd.evilcorp.atox.ui.userprofile.components.AvatarSourceDialog
import ltd.evilcorp.atox.ui.userprofile.components.ToxIdShareCard
import ltd.evilcorp.atox.ui.userprofile.components.StatusRow
import ltd.evilcorp.atox.ui.userprofile.components.ProfileLogoutCard
import ltd.evilcorp.atox.ui.userprofile.components.ProfileAvatarBox
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.auth.model.initials
import androidx.compose.ui.tooling.preview.Preview
import ltd.evilcorp.atox.ui.theme.AToxTheme
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    user: User?,
    toxId: String,
    avatarFile: java.io.File?,
    cropState: AvatarCropUiState = AvatarCropUiState.Idle,
    selectedImageUri: android.net.Uri?,
    onSelectedImageUriChanged: (android.net.Uri?) -> Unit,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit,
    onBack: () -> Unit = {},
    showBackButton: Boolean = true,
    onSetName: (String) -> Unit,
    onSetStatusMessage: (String) -> Unit,
    onSetStatus: (UserStatus) -> Unit,
    onLogout: () -> Unit = {},
    onAvatarChanged: () -> Unit = {},
    onResetCropState: () -> Unit = {},
    onCropAndSaveAvatar: (android.graphics.Bitmap, Float, Float, Float, Float, Float) -> Unit,
    performHaptic: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val selfAvatarBitmap = remember(avatarFile) {
        if (avatarFile != null && avatarFile.exists() && avatarFile.length() > 0L) {
            try {
                android.graphics.BitmapFactory.decodeFile(avatarFile.absolutePath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    LaunchedEffect(cropState) {
        when (cropState) {
            is AvatarCropUiState.Success -> {
                onAvatarChanged()
                onResetCropState()
                onSelectedImageUriChanged(null)
            }
            is AvatarCropUiState.Failure -> {
                Toast.makeText(context, context.getString(R.string.avatar_too_large), Toast.LENGTH_LONG).show()
                onResetCropState()
                onSelectedImageUriChanged(null)
            }
            else -> {}
        }
    }

    var showSourceDialog by remember { mutableStateOf(false) }

    var tempName by remember { mutableStateOf("") }
    var tempStatus by remember { mutableStateOf("") }
    var nameHasFocus by remember { mutableStateOf(false) }
    var statusHasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let {
            if (!nameHasFocus) {
                tempName = it.name
            }
            if (!statusHasFocus) {
                tempStatus = it.statusMessage
            }
        }
    }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    val profileContent = @Composable { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Beautiful Material 3 Profile Picture with Edit Button
            ProfileAvatarBox(
                selfAvatarBitmap = selfAvatarBitmap,
                user = user,
                onAvatarClick = { showSourceDialog = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // User Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.profile_edit_data),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = {
                            tempName = it
                            onSetName(it)
                        },
                        label = { Text(stringResource(R.string.name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { nameHasFocus = it.isFocused }
                    )

                    OutlinedTextField(
                        value = tempStatus,
                        onValueChange = {
                            tempStatus = it
                            onSetStatusMessage(it)
                        },
                        label = { Text(stringResource(R.string.status_message)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { statusHasFocus = it.isFocused }
                    )
                }
            }

            // Tox Status Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.profile_select_status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val activeStatus = user?.status ?: UserStatus.None

                    // Status rows
                    StatusRow(
                        title = stringResource(R.string.status_available),
                        color = StatusAvailable,
                        isSelected = activeStatus == UserStatus.None,
                        onClick = {
                            performHaptic()
                            onSetStatus(UserStatus.None)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    StatusRow(
                        title = stringResource(R.string.status_away),
                        color = StatusAway,
                        isSelected = activeStatus == UserStatus.Away,
                        onClick = {
                            performHaptic()
                            onSetStatus(UserStatus.Away)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    StatusRow(
                        title = stringResource(R.string.status_busy),
                        color = StatusBusy,
                        isSelected = activeStatus == UserStatus.Busy,
                        onClick = {
                            performHaptic()
                            onSetStatus(UserStatus.Busy)
                        }
                    )
                }
            }

            // Tox ID Card
            ToxIdShareCard(
                toxId = toxId,
                onCopyClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Tox ID", toxId)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.profile_copied), Toast.LENGTH_SHORT).show()
                },
                onShareClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "tox:$toxId")
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.tox_id_share)))
                },
                onQrClick = { showQrDialog = true }
            )

            // Logout Card
            ProfileLogoutCard(onLogoutClick = { showLogoutConfirmDialog = true })

            val extraBottomSpacer = 32.dp
            Spacer(modifier = Modifier.height(extraBottomSpacer + bottomPadding))
        }
    }

    if (showBackButton) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.profile), fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        ) { paddingValues ->
            profileContent(paddingValues)
        }
    } else {
        profileContent(PaddingValues(0.dp))
    }

    if (showLogoutConfirmDialog) {
        AtoxConfirmDialog(
            onDismiss = { showLogoutConfirmDialog = false },
            onConfirm = onLogout,
            title = stringResource(R.string.profile_logout_confirm_title),
            text = stringResource(R.string.profile_logout_confirm),
            confirmText = stringResource(R.string.profile_logout_confirm_button),
            dismissText = stringResource(R.string.profile_logout_cancel_button),
            isDangerous = true
        )
    }

    if (showQrDialog) {
        QrCodeDialog(
            toxId = toxId,
            onDismiss = { showQrDialog = false }
        )
    }

    if (cropState is AvatarCropUiState.Processing) {
        AvatarProcessingDialog()
    }

    if (selectedImageUri != null && cropState !is AvatarCropUiState.Processing) {
        AvatarEditDialog(
            imageUri = selectedImageUri!!,
            onDismiss = { onSelectedImageUriChanged(null) },
            onConfirm = { originalBitmap, scale, offsetX, offsetY, rotation, viewportWidth ->
                onCropAndSaveAvatar(originalBitmap, scale, offsetX, offsetY, rotation, viewportWidth)
            }
        )
    }

    if (showSourceDialog) {
        AvatarSourceDialog(
            onDismissRequest = { showSourceDialog = false },
            onCameraClick = {
                showSourceDialog = false
                onLaunchCamera()
            },
            onGalleryClick = {
                showSourceDialog = false
                onLaunchGallery()
            }
        )
    }
}

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
            avatarFile = null,
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
