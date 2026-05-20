package ltd.evilcorp.atox.ui.userprofile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Refresh
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Matrix
import io.nayuki.qrcodegen.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.theme.StatusAvailable
import ltd.evilcorp.atox.ui.theme.StatusAway
import ltd.evilcorp.atox.ui.theme.StatusBusy
import ltd.evilcorp.core.model.User
import ltd.evilcorp.core.model.UserStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userState: State<User?>,
    toxId: String,
    onBack: () -> Unit = {},
    showBackButton: Boolean = true,
    onSetName: (String) -> Unit,
    onSetStatusMessage: (String) -> Unit,
    onSetStatus: (UserStatus) -> Unit,
    onLogout: () -> Unit = {},
    onAvatarChanged: () -> Unit = {}
) {
    val user = userState.value
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var selfAvatarVersion by remember { mutableStateOf(0) }
    val filesDir = context.filesDir
    val selfAvatarFile = remember { java.io.File(filesDir, "self_avatar.png") }
    val selfAvatarBitmap = remember(selfAvatarVersion) {
        if (selfAvatarFile.exists() && selfAvatarFile.length() > 0L) {
            try {
                android.graphics.BitmapFactory.decodeFile(selfAvatarFile.absolutePath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    var tempName by remember(user?.name) { mutableStateOf(user?.name ?: "") }
    var tempStatus by remember(user?.statusMessage) { mutableStateOf(user?.statusMessage ?: "") }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Beautiful Material 3 Profile Picture with Edit Button
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selfAvatarBitmap != null) {
                        Image(
                            bitmap = selfAvatarBitmap,
                            contentDescription = stringResource(R.string.profile_photo_description),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        val initials = remember(user?.name) {
                            val name = user?.name ?: ""
                            if (name.isEmpty()) "U" else {
                                val parts = name.split(" ")
                                if (parts.size == 1) name.take(1) else name.take(1) + parts[1].take(1)
                            }
                        }
                        Text(
                            text = initials.uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { launcher.launch("image/*") }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.profile_avatar_change),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

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
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
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
                        onClick = { onSetStatus(UserStatus.None) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    StatusRow(
                        title = stringResource(R.string.status_away),
                        color = StatusAway,
                        isSelected = activeStatus == UserStatus.Away,
                        onClick = { onSetStatus(UserStatus.Away) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    StatusRow(
                        title = stringResource(R.string.status_busy),
                        color = StatusBusy,
                        isSelected = activeStatus == UserStatus.Busy,
                        onClick = { onSetStatus(UserStatus.Busy) }
                    )
                }
            }

            // Tox ID Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.profile_your_tox_id),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.profile_share_tox_id_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tox ID Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = toxId,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Tox ID", toxId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(R.string.profile_copied), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.copy_atox),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, "tox:$toxId")
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.tox_id_share)))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.share),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                        }

                        FilledTonalButton(
                            onClick = { showQrDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = "QR Code")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.read_qr),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Logout Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.profile_logout),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.profile_logout_confirm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showLogoutConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.profile_logout), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text(stringResource(R.string.profile_logout_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.profile_logout_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.profile_logout_confirm_button), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text(stringResource(R.string.profile_logout_cancel_button))
                }
            }
        )
    }

    if (showQrDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { showQrDialog = false },
            title = { Text(stringResource(R.string.read_qr), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.profile_share_tox_id_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(200.dp)
                            .padding(8.dp)
                    ) {
                        QrCodeView(
                            text = "tox:$toxId",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentColor = Color.Black
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showQrDialog = false }
                ) {
                    Text(stringResource(android.R.string.ok), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (selectedImageUri != null) {
        AvatarEditDialog(
            imageUri = selectedImageUri!!,
            onDismiss = { selectedImageUri = null },
            onConfirm = { croppedBitmap ->
                val maxBytes = 64 * 1024
                val destFile = java.io.File(context.filesDir, "self_avatar.png")
                var quality = 90
                var success = false

                while (quality > 10) {
                    val bos = java.io.ByteArrayOutputStream()
                    croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, bos)
                    val bytes = bos.toByteArray()
                    if (bytes.size <= maxBytes) {
                        destFile.writeBytes(bytes)
                        success = true
                        break
                    }
                    quality -= 10
                }

                if (!success) {
                    val bos = java.io.ByteArrayOutputStream()
                    croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 10, bos)
                    val bytes = bos.toByteArray()
                    if (bytes.size <= maxBytes) {
                        destFile.writeBytes(bytes)
                        success = true
                    }
                }

                croppedBitmap.recycle()

                if (success) {
                    selfAvatarVersion++
                    onAvatarChanged()
                } else {
                    Toast.makeText(context, context.getString(R.string.avatar_too_large), Toast.LENGTH_LONG).show()
                }
                selectedImageUri = null
            }
        )
    }
}

@Composable
fun AvatarEditDialog(
    imageUri: android.net.Uri,
    onDismiss: () -> Unit,
    onConfirm: (android.graphics.Bitmap) -> Unit
) {
    val context = LocalContext.current
    val originalBitmap = remember(imageUri) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bmp
        } catch (e: Exception) {
            null
        }
    }

    if (originalBitmap == null) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var rotation by remember { mutableStateOf(0f) }
    var viewportWidth by remember { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Text(
                    text = stringResource(R.string.avatar_editor_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Viewport area with circle crop frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            viewportWidth = coordinates.size.width.toFloat()
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Loaded Bitmap rendering
                    Image(
                        bitmap = originalBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y,
                                rotationZ = rotation
                            )
                    )

                    // Circular visiere overlay with semi-transparent background
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val circleRadius = 125.dp.toPx() // viewport width is ~250.dp, radius is 125.dp

                        val androidCanvas = drawContext.canvas.nativeCanvas
                        val layer = androidCanvas.saveLayer(0f, 0f, canvasWidth, canvasHeight, null)
                        with(drawContext.canvas) {
                            // 1. Draw solid dark background overlay
                            drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                            
                            // 2. Punch a circle hole in the middle
                            drawCircle(
                                color = androidx.compose.ui.graphics.Color.Transparent,
                                radius = circleRadius,
                                center = center,
                                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                            )
                            
                            // 3. Draw premium white border around crop frame
                            drawCircle(
                                color = androidx.compose.ui.graphics.Color.White,
                                radius = circleRadius,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                            
                            androidCanvas.restore()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zoom slider
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.avatar_editor_zoom) + ": ${String.format("%.1fx", scale)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1f..5f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        rotation = (rotation + 90f) % 360f
                    }) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.avatar_editor_rotate))
                    }

                    TextButton(onClick = {
                        scale = 1f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                        rotation = 0f
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.avatar_editor_reset))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirmation / Dismiss buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.avatar_editor_cancel))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val cropped = cropAvatar(originalBitmap, scale, offset, rotation, if (viewportWidth > 0f) viewportWidth else 500f)
                            onConfirm(cropped)
                        }
                    ) {
                        Text(stringResource(R.string.avatar_editor_save))
                    }
                }
            }
        }
    }
}

fun cropAvatar(bitmap: Bitmap, scale: Float, offset: androidx.compose.ui.geometry.Offset, rotation: Float, viewportWidth: Float): Bitmap {
    val cropped = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(cropped)
    val matrix = android.graphics.Matrix()
    
    // 1. Center the original bitmap in the origin space
    matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
    
    // 2. Base scale: fit the shortest dimension to the viewport
    val fitScale = viewportWidth / Math.min(bitmap.width, bitmap.height)
    val totalScale = fitScale * scale * (256f / viewportWidth)
    matrix.postScale(totalScale, totalScale)
    
    // 3. Rotation
    matrix.postRotate(rotation)
    
    // 4. Translate by user offset scaled to 256x256 target coordinates and center at (128, 128)
    val scaleFactor = 256f / viewportWidth
    matrix.postTranslate(128f + offset.x * scaleFactor, 128f + offset.y * scaleFactor)
    
    val paint = android.graphics.Paint().apply {
        isFilterBitmap = true
    }
    canvas.drawBitmap(bitmap, matrix, paint)
    return cropped
}

@Composable
fun StatusRow(
    title: String,
    color: androidx.compose.ui.graphics.Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun QrCodeView(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.Black
) {
    val qr = remember(text) {
        try {
            QrCode.encodeText(text, QrCode.Ecc.MEDIUM)
        } catch (e: Exception) {
            null
        }
    }

    if (qr != null) {
        Canvas(modifier = modifier) {
            val size = qr.size
            val cellSize = this.size.width / size
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (qr.getModule(x, y)) {
                        drawRect(
                            color = contentColor,
                            topLeft = androidx.compose.ui.geometry.Offset(x * cellSize, y * cellSize),
                            size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

fun compressAndSaveAvatar(context: Context, uri: android.net.Uri): Boolean {
    val maxBytes = 64 * 1024
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return false
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return false
        inputStream.close()

        val maxDim = 256
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaledBitmap = if (width > maxDim || height > maxDim) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
            val newHeight = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
            android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        val destFile = java.io.File(context.filesDir, "self_avatar.png")
        var quality = 90
        var success = false

        while (quality > 10) {
            val bos = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, bos)
            val bytes = bos.toByteArray()
            if (bytes.size <= maxBytes) {
                destFile.writeBytes(bytes)
                success = true
                break
            }
            quality -= 10
        }

        if (!success) {
            val bos = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 10, bos)
            val bytes = bos.toByteArray()
            if (bytes.size <= maxBytes) {
                destFile.writeBytes(bytes)
                success = true
            }
        }

        if (scaledBitmap != originalBitmap) {
            scaledBitmap.recycle()
        }
        originalBitmap.recycle()
        return success
    } catch (e: Exception) {
        android.util.Log.e("UserProfileScreen", "Failed to compress/save avatar", e)
        return false
    }
}
