// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.userprofile.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R

private const val COLOR_PREMIUM_VIOLET = 0xFF1B1429L
private const val COLOR_DEEP_INDIGO = 0xFF0F0A1AL
private const val COLOR_MIDNIGHT_BLACK = 0xFF08040FL
private const val COLOR_BLUE_ACCENT = 0xFF2196F3L
private const val DEFAULT_VIEWPORT_WIDTH = 500f

@Composable
fun AvatarEditDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (originalBitmap: Bitmap, scale: Float, offsetX: Float, offsetY: Float, rotation: Float, viewportWidth: Float) -> Unit
) {
    val context = LocalContext.current
    var originalBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(imageUri) { mutableStateOf(true) }

    LaunchedEffect(imageUri) {
        isLoading = true
        originalBitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                null
            }
        }
        isLoading = false
    }

    if (!isLoading && originalBitmap == null) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(COLOR_PREMIUM_VIOLET),
                            Color(COLOR_DEEP_INDIGO),
                            Color(COLOR_MIDNIGHT_BLACK)
                        )
                    )
                )
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header
                    Text(
                        text = stringResource(R.string.avatar_editor_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp)
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
                        originalBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
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
                        }

                        // Circular visor overlay with semi-transparent background
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val circleRadius = 125.dp.toPx()

                            val androidCanvas = drawContext.canvas.nativeCanvas
                            androidCanvas.saveLayer(0f, 0f, canvasWidth, canvasHeight, null)
                            with(drawContext.canvas) {
                                // 1. Draw solid dark background overlay
                                drawRect(Color.Black.copy(alpha = 0.6f))
                                
                                // 2. Punch a circle hole in the middle
                                drawCircle(
                                    color = Color.Transparent,
                                    radius = circleRadius,
                                    center = center,
                                    blendMode = BlendMode.Clear
                                )
                                
                                // 3. Draw border around crop frame
                                drawCircle(
                                    color = Color.White,
                                    radius = circleRadius,
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                
                                androidCanvas.restore()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Zoom slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.avatar_editor_zoom) + ": ${String.format(java.util.Locale.US, "%.1fx", scale)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 1f..5f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(COLOR_BLUE_ACCENT),
                                activeTrackColor = Color(COLOR_BLUE_ACCENT),
                                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                            ),
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
                        TextButton(
                            onClick = { rotation = (rotation + 90f) % 360f },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.avatar_editor_rotate))
                        }

                        TextButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                                rotation = 0f
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
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
                        TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))) {
                            Text(stringResource(R.string.avatar_editor_cancel), fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                originalBitmap?.let { bmp ->
                                    onConfirm(
                                        bmp,
                                        scale,
                                        offset.x,
                                        offset.y,
                                        rotation,
                                        if (viewportWidth > 0f) viewportWidth else DEFAULT_VIEWPORT_WIDTH
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(COLOR_BLUE_ACCENT), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.avatar_editor_save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarProcessingDialog() {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text(stringResource(R.string.settings_cache_calculating)) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    )
}
