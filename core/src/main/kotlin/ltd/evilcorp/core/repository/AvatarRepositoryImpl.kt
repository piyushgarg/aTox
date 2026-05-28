package ltd.evilcorp.core.repository

import android.content.Context
import ltd.evilcorp.domain.features.auth.repository.IAvatarRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarRepositoryImpl @Inject constructor(
    private val context: Context
) : IAvatarRepository {
    override suspend fun saveSelfAvatar(bytes: ByteArray): Boolean {
        return try {
            val destFile = getSelfAvatarFile()
            destFile.writeBytes(bytes)
            true
        } catch (e: Exception) {
            android.util.Log.e("AvatarRepositoryImpl", "Failed to save avatar", e)
            false
        }
    }

    override fun getSelfAvatarFile(): File {
        val oldFile = File(context.filesDir, "self_avatar.png")
        val newFile = File(context.filesDir, "self_avatar.jpg")
        if (oldFile.exists() && !newFile.exists()) {
            try {
                oldFile.renameTo(newFile)
            } catch (e: Exception) {
                android.util.Log.e("AvatarRepositoryImpl", "Failed to migrate self_avatar.png to self_avatar.jpg", e)
            }
        }
        return newFile
    }
}
