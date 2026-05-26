package ltd.evilcorp.core.emulator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.RingtoneManager
import kotlinx.coroutines.delay
import ltd.evilcorp.domain.feature.IGroupFileTransferEmulator
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupFileTransferEmulatorImpl @Inject constructor(
    private val context: Context
) : IGroupFileTransferEmulator {

    override suspend fun emulateDownload(
        id: Int,
        fileName: String,
        fileSize: Long,
        onProgress: suspend (Long) -> Unit
    ): ByteArray? {
        onProgress(0L)
        
        val totalSize = fileSize
        val speedBytesPerSec = 700 * 1024 // ~700 KB/s
        val estimatedDurationMs = (totalSize.toFloat() / speedBytesPerSec * 1000).toLong()
        val totalDurationMs = maxOf(1000L, minOf(25000L, estimatedDurationMs))
        val stepDelay = 250L
        val steps = (totalDurationMs / stepDelay).toInt().coerceAtLeast(5)
        val stepSize = totalSize / steps
        
        for (i in 1..steps) {
            delay(stepDelay)
            val currentProgress = if (i == steps) totalSize else i * stepSize
            onProgress(currentProgress)
        }
        
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val isImage = ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
        val isVoice = fileName.startsWith("voice_message_")
        
        if (isVoice) {
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (defaultUri != null) {
                    context.contentResolver.openInputStream(defaultUri)?.use { input ->
                        return input.readBytes()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (isImage) {
            try {
                val width = 512
                val height = 512
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                
                val paint = Paint()
                val shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    Color.parseColor("#8A2BE2"),
                    Color.parseColor("#FF69B4"),
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 40f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("aTox Image Transfer", width / 2f, height / 2f - 20f, textPaint)
                
                val subPaint = Paint().apply {
                    color = Color.argb(180, 255, 255, 255)
                    textSize = 24f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(fileName, width / 2f, height / 2f + 40f, subPaint)
                
                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
                val bytes = bos.toByteArray()
                bitmap.recycle()
                return bytes
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return "Mock received file content".toByteArray(Charsets.UTF_8)
    }
}
