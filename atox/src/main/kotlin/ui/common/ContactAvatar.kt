package ltd.evilcorp.atox.ui.common

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.theme.ContactBackgrounds
import ltd.evilcorp.atox.ui.theme.avatarContentColor
import kotlin.math.abs

private val avatarBitmapCache = LruCache<String, ImageBitmap>(64)

@Composable
fun ContactAvatar(
    name: String,
    publicKey: String,
    avatarUri: String,
    size: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val fallbackName = stringResource(R.string.contact_default_name)
    val displayName = name.ifEmpty { fallbackName }
    val initials = remember(displayName) {
        val segments = displayName.split(" ").filter { it.isNotBlank() }
        when {
            segments.isEmpty() -> fallbackName.take(1)
            segments.size == 1 -> segments.first().take(1)
            else -> segments.first().take(1) + segments[1].take(1)
        }
    }
    val avatarColor = remember(publicKey) {
        ContactBackgrounds[abs(publicKey.hashCode()).rem(ContactBackgrounds.size)]
    }
    val lastModified = remember(avatarUri) {
        avatarUri.takeIf { it.isNotEmpty() }?.let {
            runCatching {
                val file = Uri.parse(it).path?.let(::File)
                if (file != null && file.exists()) {
                    file.lastModified()
                } else 0L
            }.getOrDefault(0L)
        } ?: 0L
    }

    val initialBitmap = remember(avatarUri, lastModified) {
        if (avatarUri.isEmpty()) null
        else {
            val cacheKey = "$avatarUri:$lastModified"
            avatarBitmapCache.get(cacheKey)
        }
    }

    val avatarBitmap by produceState<ImageBitmap?>(initialValue = initialBitmap, avatarUri, lastModified) {
        value = if (avatarUri.isEmpty()) {
            null
        } else {
            val cacheKey = "$avatarUri:$lastModified"
            avatarBitmapCache.get(cacheKey) ?: withContext(Dispatchers.IO) {
                runCatching {
                    val file = Uri.parse(avatarUri).path?.let(::File)
                    if (file != null && file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()?.also {
                            avatarBitmapCache.put(cacheKey, it)
                        }
                    } else {
                        null
                    }
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = avatarBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.profile_photo_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = initials.uppercase(),
                color = avatarContentColor(avatarColor),
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
