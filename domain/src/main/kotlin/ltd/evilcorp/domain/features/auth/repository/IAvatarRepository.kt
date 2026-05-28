package ltd.evilcorp.domain.features.auth.repository

interface IAvatarRepository {
    suspend fun saveSelfAvatar(bytes: ByteArray): Boolean
    fun getSelfAvatarFile(): java.io.File
}
