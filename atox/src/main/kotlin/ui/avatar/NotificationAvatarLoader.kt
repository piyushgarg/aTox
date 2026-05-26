// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.avatar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationAvatarLoader @Inject constructor() {
    private val circleTransform = object : Transformation {
        override fun transform(bitmap: Bitmap): Bitmap {
            val output = createBitmap(bitmap.width, bitmap.height)
            val canvas = Canvas(output)
            val paint = Paint()
            val rect = Rect(0, 0, bitmap.width, bitmap.height)

            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawCircle(bitmap.width / 2.0f, bitmap.height / 2.0f, bitmap.width / 2.0f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            if (bitmap != output) {
                bitmap.recycle()
            }
            return output
        }

        override fun key() = "circleTransform"
    }

    fun loadAvatar(avatarUri: String): Bitmap? {
        if (avatarUri.isEmpty()) return null
        return try {
            Picasso.get().load(avatarUri).transform(circleTransform).get()
        } catch (e: Exception) {
            null
        }
    }

    fun invalidateAvatar(uri: android.net.Uri) {
        Picasso.get().invalidate(uri)
    }
}
