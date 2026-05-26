package ltd.evilcorp.core.repository

import android.content.Context
import ltd.evilcorp.domain.repository.IAvatarStorage
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarStorageImpl @Inject constructor(
    private val context: Context
) : IAvatarStorage {
    override suspend fun saveSelfAvatar(bytes: ByteArray): Boolean {
        return try {
            val destFile = File(context.filesDir, "self_avatar.png")
            destFile.writeBytes(bytes)
            true
        } catch (e: Exception) {
            android.util.Log.e("AvatarStorageImpl", "Failed to save avatar", e)
            false
        }
    }
}
