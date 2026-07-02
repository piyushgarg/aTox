package ltd.evilcorp.atox.ui.common.chat

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.LocalFileStorageProvider

private const val COMPRESSION_QUALITY = 95

@Composable
fun FileImageViewerDialog(
    destination: String,
    fileName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fileStorageProvider = LocalFileStorageProvider.current
    val scope = rememberCoroutineScope()

    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, destination) {
        value = withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(destination)
                val inputStream = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    context.contentResolver.openInputStream(uri)
                } else {
                    val path = fileStorageProvider.getAbsolutePath(destination)
                    if (path != null && fileStorageProvider.exists(destination)) {
                        java.io.FileInputStream(path)
                    } else null
                }
                inputStream?.use { stream ->
                    val bytes = stream.readBytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            } catch (e: Exception) {
                android.util.Log.e("FileImageViewerDialog", "Failed to load image", e)
                null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            imageBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imageBitmap?.let { bitmap ->
                    IconButton(
                        onClick = {
                            scope.launch {
                                saveImageToGallery(context, bitmap, fileName)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Save to gallery",
                            tint = Color.White
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private suspend fun saveImageToGallery(context: android.content.Context, imageBitmap: ImageBitmap, fileName: String) {
    withContext(Dispatchers.IO) {
        try {
            val bitmap = imageBitmap.asAndroidBitmap()
            val displayName = fileName.substringBeforeLast('.').ifEmpty { "image_${System.currentTimeMillis()}" }
            val mimeType = "image/${fileName.substringAfterLast('.', "jpg")}"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val imageCollectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val insertedUri = resolver.insert(imageCollectionUri, contentValues)
            if (insertedUri != null) {
                resolver.openOutputStream(insertedUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(insertedUri, contentValues, null, null)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.file_saved_to_gallery, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FileImageViewerDialog", "Failed to save image", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.file_save_error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
