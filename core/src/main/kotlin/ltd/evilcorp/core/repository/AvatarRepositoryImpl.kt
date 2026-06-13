package ltd.evilcorp.core.repository

import android.content.Context
import ltd.evilcorp.domain.features.auth.repository.IAvatarRepository
import ltd.evilcorp.core.profile.ProfileManager
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
        val activeProfileId = ProfileManager.getActiveProfileId(context)
        val filename = if (activeProfileId == ProfileManager.DEFAULT_PROFILE_ID) "self_avatar.jpg" else "self_avatar_$activeProfileId.jpg"
        val oldFilename = if (activeProfileId == ProfileManager.DEFAULT_PROFILE_ID) "self_avatar.png" else "self_avatar_$activeProfileId.png"
        
        val oldFile = File(context.filesDir, oldFilename)
        val newFile = File(context.filesDir, filename)
        if (oldFile.exists() && !newFile.exists()) {
            try {
                oldFile.renameTo(newFile)
            } catch (e: Exception) {
                android.util.Log.e("AvatarRepositoryImpl", "Failed to migrate $oldFilename to $filename", e)
            }
        }
        return newFile
    }
}
