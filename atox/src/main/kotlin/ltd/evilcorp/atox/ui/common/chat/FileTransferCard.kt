// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import android.content.Context
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.transfer.model.isComplete
import ltd.evilcorp.domain.features.transfer.model.isRejected
import ltd.evilcorp.domain.features.transfer.model.isStarted

private const val REQ_IMAGE_SIZE = 512
private const val MAX_WIDTH_DP = 260
private const val THUMBNAIL_ROUNDED_CORNER = 8

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
    val isLocalReady = ft.isComplete() || ft.outgoing
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

                        val reqWidth = REQ_IMAGE_SIZE
                        val reqHeight = REQ_IMAGE_SIZE
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
                    android.util.Log.e("FileTransferCard", "Failed to decode image preview", e)
                    null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .widthIn(max = MAX_WIDTH_DP.dp)
            .clip(RoundedCornerShape(THUMBNAIL_ROUNDED_CORNER.dp))
            .clickable(enabled = isLocalReady && !isRejected) { onOpenFile(ft) }
            .padding(vertical = 4.dp)
    ) {
        FileImageThumbnail(
            isComplete = isComplete,
            isImage = isImage,
            imageBitmap = imageBitmap,
            fileName = ft.fileName
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FileTransferStatusIcon(
                isRejected = isRejected,
                isLocalReady = isLocalReady,
                contentColor = contentColor
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
                FileTransferStatusText(
                    isRejected = isRejected,
                    isLocalReady = isLocalReady,
                    isStarted = isStarted,
                    isOutgoing = isOutgoing,
                    fileSize = ft.fileSize,
                    progress = ft.progress,
                    context = context,
                    contentColor = contentColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            FileTransferActionButtons(
                isRejected = isRejected,
                isStarted = isStarted,
                isComplete = isComplete,
                isOutgoing = isOutgoing,
                id = ft.id,
                fileName = ft.fileName,
                destination = ft.destination,
                msg = msg,
                onHaptic = onHaptic,
                onAcceptFt = onAcceptFt,
                onRejectFt = onRejectFt,
                onCancelFt = onCancelFt,
                onSaveAsClick = onSaveAsClick,
                contentColor = contentColor
            )
        }

        FileTransferProgress(
            isComplete = isComplete,
            isStarted = isStarted,
            isRejected = isRejected,
            isOutgoing = isOutgoing,
            progress = ft.progress,
            fileSize = ft.fileSize,
            contentColor = contentColor
        )
    }
}
