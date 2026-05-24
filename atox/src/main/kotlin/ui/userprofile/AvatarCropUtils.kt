package ltd.evilcorp.atox.ui.userprofile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import java.io.File

object AvatarCropUtils {
    private const val AVATAR_SIZE = 256
    private const val AVATAR_SIZE_F = 256f
    private const val AVATAR_CENTER_F = 128f
    private const val KB = 1024
    private const val MAX_AVATAR_BYTES = 64 * KB
    private const val INITIAL_QUALITY = 90
    private const val MIN_QUALITY = 10
    private const val QUALITY_STEP = 10

    fun cropAvatar(
        bitmap: Bitmap,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float,
        viewportWidth: Float
    ): Bitmap {
        val cropped = Bitmap.createBitmap(AVATAR_SIZE, AVATAR_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cropped)
        val matrix = Matrix()
        
        // 1. Center the original bitmap in the origin space
        matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
        
        // 2. Base scale: fit the shortest dimension to the viewport
        val fitScale = viewportWidth / Math.min(bitmap.width, bitmap.height)
        val totalScale = fitScale * scale * (AVATAR_SIZE_F / viewportWidth)
        matrix.postScale(totalScale, totalScale)
        
        // 3. Rotation
        matrix.postRotate(rotation)
        
        // 4. Translate by user offset scaled to 256x256 target coordinates and center at (128, 128)
        val scaleFactor = AVATAR_SIZE_F / viewportWidth
        matrix.postTranslate(AVATAR_CENTER_F + offsetX * scaleFactor, AVATAR_CENTER_F + offsetY * scaleFactor)
        
        val paint = Paint().apply {
            isFilterBitmap = true
        }
        canvas.drawBitmap(bitmap, matrix, paint)
        return cropped
    }

    fun saveAvatar(
        croppedBitmap: Bitmap,
        destFile: File
    ): Boolean {
        var quality = INITIAL_QUALITY
        var success = false

        try {
            while (quality > MIN_QUALITY) {
                ByteArrayOutputStream().use { bos ->
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                    val bytes = bos.toByteArray()
                    if (bytes.size <= MAX_AVATAR_BYTES) {
                        destFile.writeBytes(bytes)
                        success = true
                        return true
                    }
                }
                quality -= QUALITY_STEP
            }

            if (!success) {
                ByteArrayOutputStream().use { bos ->
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, MIN_QUALITY, bos)
                    val bytes = bos.toByteArray()
                    if (bytes.size <= MAX_AVATAR_BYTES) {
                        destFile.writeBytes(bytes)
                        success = true
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AvatarCropUtils", "Failed to save avatar", e)
        } finally {
            croppedBitmap.recycle()
        }
        return success
    }
}
