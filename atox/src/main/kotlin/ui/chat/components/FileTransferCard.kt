// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.content.ContentResolver
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.core.model.FileTransfer
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.isComplete
import ltd.evilcorp.core.model.isRejected
import ltd.evilcorp.core.model.isStarted

@Composable
fun FileTransferCard(
    ft: FileTransfer,
    msg: Message,
    onHaptic: () -> Unit,
    contentColor: Color,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (Message) -> Unit,
    onSaveAsClick: (Int, String) -> Unit,
    onOpenFile: (FileTransfer) -> Unit
) {
    val isComplete = ft.isComplete()
    val isStarted = ft.isStarted()
    val isRejected = ft.isRejected()
    val isOutgoing = ft.outgoing
    val context = LocalContext.current

    val isImage = remember(ft.fileName) {
        val ext = ft.fileName.substringAfterLast('.', "").lowercase()
        ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    }

    val lastModified = remember(ft.destination, isComplete) {
        if (isComplete && ft.destination.isNotEmpty()) {
            runCatching {
                val uri = Uri.parse(ft.destination)
                if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    0L
                } else {
                    val file = uri.path?.let(::File)
                    if (file != null && file.exists()) {
                        file.lastModified()
                    } else 0L
                }
            }.getOrDefault(0L)
        } else 0L
    }

    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, ft.destination, isComplete, lastModified) {
        value = if (!isComplete || !isImage || ft.destination.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(ft.destination)
                    val inputStream = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                        context.contentResolver.openInputStream(uri)
                    } else {
                        val path = uri.path
                        if (path != null && File(path).exists()) {
                            java.io.FileInputStream(File(path))
                        } else null
                    }
                    inputStream?.use { stream ->
                        val tempBytes = stream.readBytes()
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, options)

                        val reqWidth = 512
                        val reqHeight = 512
                        var inSampleSize = 1
                        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                            val halfHeight = options.outHeight / 2
                            val halfWidth = options.outWidth / 2
                            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                                inSampleSize *= 2
                            }
                        }

                        val decodeOptions = BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize
                        }
                        BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, decodeOptions)?.asImageBitmap()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isComplete && !isRejected) { onOpenFile(ft) }
            .padding(vertical = 4.dp)
    ) {
        if (isComplete && isImage && imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = ft.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(bottom = 6.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = when {
                    isRejected -> Icons.Default.ErrorOutline
                    isComplete -> Icons.AutoMirrored.Filled.InsertDriveFile
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ft.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        isRejected -> stringResource(R.string.ft_status_canceled)
                        isComplete -> {
                            val status = if (isOutgoing) stringResource(R.string.ft_status_sent) else stringResource(R.string.ft_status_received)
                            "${formatSize(context, ft.fileSize)} • $status"
                        }
                        !isStarted -> {
                            if (isOutgoing) stringResource(R.string.ft_status_waiting) 
                            else stringResource(R.string.ft_status_incoming, formatSize(context, ft.fileSize))
                        }
                        else -> stringResource(R.string.ft_status_progress, formatSize(context, ft.progress), formatSize(context, ft.fileSize))
                    },
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            when {
                isRejected -> {
                    IconButton(
                        onClick = {
                            onHaptic()
                            onCancelFt(msg)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                !isStarted -> {
                    if (isOutgoing) {
                        IconButton(
                            onClick = {
                                onHaptic()
                                onCancelFt(msg)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = contentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Row {
                            IconButton(
                                onClick = {
                                    onHaptic()
                                    onRejectFt(ft.id)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Decline",
                                    tint = contentColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    onHaptic()
                                    onAcceptFt(ft.id)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Accept",
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                !isComplete -> {
                    IconButton(
                        onClick = {
                            onHaptic()
                            onRejectFt(ft.id)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                isComplete -> {
                    val isAlreadySaved = ft.destination.startsWith("content://")
                    if (isAlreadySaved) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.saved),
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                onHaptic()
                                onSaveAsClick(ft.id, ft.fileName)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Save As",
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!isComplete && isStarted && !isRejected) {
            Spacer(modifier = Modifier.height(8.dp))
            val progressFraction = if (ft.fileSize > 0) ft.progress.toFloat() / ft.fileSize.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOutgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                trackColor = contentColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatSize(context: android.content.Context, size: Long): String {
    if (size <= 0) return "0 ${context.getString(R.string.size_bytes)}"
    val units = arrayOf(
        context.getString(R.string.size_bytes),
        context.getString(R.string.size_kb),
        context.getString(R.string.size_mb),
        context.getString(R.string.size_gb)
    )
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
