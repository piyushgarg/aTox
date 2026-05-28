package ltd.evilcorp.atox.ui.common.chat

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.chat.model.Message

private const val BYTES_IN_KB = 1024.0
private const val MAX_HEIGHT_DP = 160
private const val THUMBNAIL_ROUNDED_CORNER = 8
private const val THUMBNAIL_PADDING_BOTTOM = 6

@Composable
fun FileImageThumbnail(
    isComplete: Boolean,
    isImage: Boolean,
    imageBitmap: ImageBitmap?,
    fileName: String
) {
    if (isComplete && isImage && imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = MAX_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(THUMBNAIL_ROUNDED_CORNER.dp))
                .padding(bottom = THUMBNAIL_PADDING_BOTTOM.dp)
        )
    }
}

@Composable
fun FileTransferStatusIcon(
    isRejected: Boolean,
    isLocalReady: Boolean,
    contentColor: Color
) {
    Icon(
        imageVector = when {
            isRejected -> Icons.Default.ErrorOutline
            isLocalReady -> Icons.AutoMirrored.Filled.InsertDriveFile
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        },
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(36.dp)
    )
}

@Composable
fun FileTransferStatusText(
    isRejected: Boolean,
    isLocalReady: Boolean,
    isStarted: Boolean,
    isOutgoing: Boolean,
    fileSize: Long,
    progress: Long,
    context: Context,
    contentColor: Color
) {
    Text(
        text = when {
            isRejected -> stringResource(R.string.ft_status_canceled)
            isLocalReady -> {
                val status = if (isOutgoing) stringResource(R.string.ft_status_sent) else stringResource(R.string.ft_status_received)
                "${formatSize(context, fileSize)} • $status"
            }
            !isStarted -> {
                if (isOutgoing) stringResource(R.string.ft_status_waiting)
                else stringResource(R.string.ft_status_incoming, formatSize(context, fileSize))
            }
            else -> stringResource(R.string.ft_status_progress, formatSize(context, progress), formatSize(context, fileSize))
        },
        fontSize = 11.sp,
        color = contentColor.copy(alpha = 0.7f)
    )
}

@Composable
fun FileTransferActionButtons(
    isRejected: Boolean,
    isStarted: Boolean,
    isComplete: Boolean,
    isOutgoing: Boolean,
    id: Int,
    fileName: String,
    destination: String,
    msg: Message,
    onHaptic: () -> Unit,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    onCancelFt: (Message) -> Unit,
    onSaveAsClick: (Int, String) -> Unit,
    contentColor: Color
) {
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
                            onRejectFt(id)
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
                            onAcceptFt(id)
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
                    onRejectFt(id)
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
            val isAlreadySaved = destination.startsWith("content://") || isOutgoing
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
                        onSaveAsClick(id, fileName)
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

@Composable
fun FileTransferProgress(
    isComplete: Boolean,
    isStarted: Boolean,
    isRejected: Boolean,
    isOutgoing: Boolean,
    progress: Long,
    fileSize: Long,
    contentColor: Color
) {
    if (!isComplete && isStarted && !isRejected) {
        Spacer(modifier = Modifier.height(8.dp))
        val progressFraction = if (fileSize > 0) progress.toFloat() / fileSize.toFloat() else 0f
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

fun formatSize(context: Context, size: Long): String {
    if (size <= 0) return "0 ${context.getString(R.string.size_bytes)}"
    val units = arrayOf(
        context.getString(R.string.size_bytes),
        context.getString(R.string.size_kb),
        context.getString(R.string.size_mb),
        context.getString(R.string.size_gb)
    )
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(BYTES_IN_KB)).toInt()
    return java.text.DecimalFormat("#,##0.#").format(size / Math.pow(BYTES_IN_KB, digitGroups.toDouble())) + " " + units[digitGroups]
}
